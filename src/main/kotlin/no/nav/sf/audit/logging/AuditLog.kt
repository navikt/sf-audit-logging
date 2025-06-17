package no.nav.sf.audit.logging

import mu.KotlinLogging
import no.nav.sf.audit.logging.db.DefaultPostgresDatabase
import no.nav.sf.audit.logging.db.MockPostgresDatabase
import no.nav.sf.audit.logging.db.PostgresDatabase
import no.nav.sf.audit.logging.salesforce.DefaultSalesforceClient
import no.nav.sf.audit.logging.salesforce.PersonIdentsResponse
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import no.nav.sf.audit.logging.salesforce.UriEvent
import java.time.LocalDate

class AuditLog(private val entity: String = "All", private val salesforceClient: SalesforceClient = DefaultSalesforceClient(), private val postgresDatabase: PostgresDatabase = if (local) MockPostgresDatabase() else DefaultPostgresDatabase()) {
    private val uriEventFilterHelper = UriEventFilterHelper(entity)
    private val log = KotlinLogging.logger { }
    private val naudit = KotlinLogging.logger("AuditLogger")

    fun fetchAndLog(eventDate: LocalDate): Int {
        var totalNumberOfLoggedRecords = 0
        var totalNumberOfApiCalls = 0

        val successfulLoggedForEventDate = if (entity == "All") postgresDatabase.fetchAuditLogSyncStatus(eventDate) else postgresDatabase.fetchAuditLogSyncStatusByEntity(eventDate, entity)
        if (successfulLoggedForEventDate.isNotEmpty()) {
            // Stop if we have already logged audit logs for the event date
            log.warn { "Audit logs have already been logged for $eventDate" }
            return 0
        }
        log.info { "Fetch and log audit logs for $eventDate" }
        Metrics.clearUriEventsCounter()
        val filteredUriEvents = uriEventFilterHelper.filterUriEventsToHaveObjectsToBeLogged(salesforceClient.fetchUriEvents(eventDate))
        log.info { "Filtered ${filteredUriEvents.size} URI events" }
        filteredUriEvents.groupBy { it.entity }.forEach { (entity, events) ->
            val personIdentsResponse = salesforceClient.fetchPersonIdents(
                objectName = entity,
                personIdentSelectClause = uriEventFilterHelper.objectsToBeLogged.getProperty(entity),
                recordIds = events.map { it.recordId }
            )
            totalNumberOfApiCalls += personIdentsResponse.numberOfApiCalls
            val (uriEventsWithPersonIdent, uriEventsWithoutAnyPersonIdents) = setUriEventsWithAndWithoutPersonIdent(events, personIdentsResponse)
            totalNumberOfLoggedRecords += uriEventsWithPersonIdent.toInt()

            if (uriEventsWithPersonIdent> 0) {
                Metrics.uriEventsWithPersonIdent.labels(entity).inc(uriEventsWithPersonIdent)
                postgresDatabase.upsertAuditLogSyncStatus(eventDate, LocalDate.now(), entity, uriEventsWithPersonIdent.toInt())
            }
            if (uriEventsWithoutAnyPersonIdents> 0) {
                Metrics.uriEventsWithoutAnyPersonIdents.labels(entity).inc(uriEventsWithoutAnyPersonIdents)
            }
        }
        if (totalNumberOfLoggedRecords == 0) {
            log.warn { "No audit logs to log for $eventDate" }
        }
        Metrics.numberOfApiCalls.labels("RequestPersonIdents").inc(totalNumberOfApiCalls.toDouble())
        return totalNumberOfLoggedRecords.toInt()
    }

    private fun setUriEventsWithAndWithoutPersonIdent(events: List<UriEvent>, personIdentsResponse: PersonIdentsResponse): Pair<Double, Double> {
        var uriEventsWithPersonIdent = 0.0
        var uriEventsWithoutAnyPersonIdents = 0.0
        events.forEach { event ->
            val personIdent = personIdentsResponse.personIdentByRecordId[event.recordId]
            if (personIdent != null) {
                event.personIdent = personIdent
                uriEventsWithPersonIdent += 1.0
                naudit.info(createLogMessage(event))
            } else {
                uriEventsWithoutAnyPersonIdents += 1.0
            }
        }
        return Pair(uriEventsWithPersonIdent, uriEventsWithoutAnyPersonIdents)
    }
}
