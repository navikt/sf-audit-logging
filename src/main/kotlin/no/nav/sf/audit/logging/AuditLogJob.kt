package no.nav.sf.audit.logging

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.sf.audit.logging.db.PostgresDatabase
import no.nav.sf.audit.logging.plugins.Metrics
import no.nav.sf.audit.logging.salesforce.PersonIdentsResponse
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import no.nav.sf.audit.logging.salesforce.UriEvent
import java.time.LocalDate

object AuditLogJob {

    var active = false
    private val log = KotlinLogging.logger { }
    private val naudit = KotlinLogging.logger("AuditLogger")

    fun activateFetchAndLog(eventDate: LocalDate, entity: String, offset: Int, salesforceClient: SalesforceClient, postgresDatabase: PostgresDatabase) {
        if (active) throw IllegalStateException("Cannot activate new job since one is already active")
        active = true
        GlobalScope.launch {
            fetchAndLog(eventDate, entity, offset, salesforceClient, postgresDatabase)
        }
    }

    fun fetchAndLog(eventDate: LocalDate, entity: String = "All", offset: Int = 0, salesforceClient: SalesforceClient, postgresDatabase: PostgresDatabase): Int {
        val uriEventFilterHelper = UriEventFilterHelper(entity)
        var totalNumberOfLoggedRecords = 0
        var totalNumberOfApiCalls = 0

        if (entity == "All" && postgresDatabase.fetchAuditLogSyncStatus(eventDate).isNotEmpty()) {
            active = false
            throw IllegalStateException("Audit logs have already been logged for $eventDate")
        }
        postgresDatabase.closeConnection()

        log.info { "Fetch and log audit logs for $eventDate" }
        try {

            Metrics.clearUriEventsCounter()
            val filteredUriEvents =
                uriEventFilterHelper.filterUriEventsToHaveObjectsToBeLogged(salesforceClient.fetchUriEvents(eventDate))
            log.info { "Filtered ${filteredUriEvents.size} URI events" }
            filteredUriEvents.groupBy { it.entity }.forEach { (entity, events) ->
                val personIdentsResponse = salesforceClient.fetchPersonIdents(
                    objectName = entity,
                    personIdentSelectClause = uriEventFilterHelper.objectsToBeLogged.getProperty(entity),
                    recordIds = events.map { it.recordId }
                )
                totalNumberOfApiCalls += personIdentsResponse.numberOfApiCalls
                val (uriEventsWithPersonIdent, uriEventsWithoutAnyPersonIdents) = setUriEventsWithAndWithoutPersonIdent(
                    events,
                    personIdentsResponse,
                    offset
                )
                totalNumberOfLoggedRecords += uriEventsWithPersonIdent.toInt()

                if (uriEventsWithPersonIdent > 0) {
                    Metrics.uriEventsWithPersonIdent.labels(entity).inc(uriEventsWithPersonIdent)
                    log.info() { "Logging ${uriEventsWithPersonIdent.toInt()} metrics entity $entity" }
                    postgresDatabase.insertAuditLogSyncStatus(
                        eventDate,
                        LocalDate.now(),
                        entity,
                        uriEventsWithPersonIdent.toInt()
                    )
                }
                if (uriEventsWithoutAnyPersonIdents > 0) {
                    Metrics.uriEventsWithoutAnyPersonIdents.labels(entity).inc(uriEventsWithoutAnyPersonIdents)
                }
            }
            if (totalNumberOfLoggedRecords == 0) {
                log.warn { "No audit logs to log for $eventDate" }
            }
            Metrics.numberOfApiCalls.labels("RequestPersonIdents").inc(totalNumberOfApiCalls.toDouble())
        } catch (e: Exception) {
            log.error { "Error while fetching and logging audit logs " + e.message }
        } finally {
            postgresDatabase.closeConnection()
            active = false
        }
        return totalNumberOfLoggedRecords.toInt()
    }

    private fun setUriEventsWithAndWithoutPersonIdent(events: List<UriEvent>, personIdentsResponse: PersonIdentsResponse, offset: Int): Pair<Double, Double> {
        var uriEventsWithPersonIdent = 0.0
        var uriEventsWithoutAnyPersonIdents = 0.0
        var batchCounter = 0
        var indexForOffset = 0
        events.forEach { event ->
            val personIdent = personIdentsResponse.personIdentByRecordId[event.recordId]
            if (personIdent != null) {
                indexForOffset++
                if (indexForOffset - 1 < offset) {
                    return@forEach // Skip this event if it is before the offset
                }
                event.personIdent = personIdent
                uriEventsWithPersonIdent += 1.0
                naudit.info(createLogMessage(event))
                batchCounter++
                if (batchCounter == 100) {
                    Thread.sleep(2000) // Pause for 2 seconds
                    batchCounter = 0
                }
            } else {
                uriEventsWithoutAnyPersonIdents += 1.0
            }
        }
        return Pair(uriEventsWithPersonIdent, uriEventsWithoutAnyPersonIdents)
    }
}
