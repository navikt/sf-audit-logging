package no.nav.sf.audit.logging

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.sf.audit.logging.db.PostgresDatabase
import no.nav.sf.audit.logging.plugins.Metrics
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import no.nav.sf.audit.logging.services.AuditLogPublisher
import no.nav.sf.audit.logging.services.AuditLogSyncJobMonitor
import no.nav.sf.audit.logging.services.EntitySelectionService
import no.nav.sf.audit.logging.services.UriEventEntityFilterService
import java.time.LocalDate

object AuditLogJob {

    var active = false
    private val log = KotlinLogging.logger { }
    private val entitySelectionService = EntitySelectionService()
    private val uriEventEntityFilterService = UriEventEntityFilterService()

    fun activateFetchAndLog(eventDate: LocalDate, entity: String, offset: Int, salesforceClient: SalesforceClient, postgresDatabase: PostgresDatabase, auditLogSyncJobMonitor: AuditLogSyncJobMonitor, auditLogPublisher: AuditLogPublisher) {
        if (active) throw IllegalStateException("Cannot activate new job since one is already active")
        active = true
        GlobalScope.launch {
            fetchAndLog(eventDate, entity, offset, salesforceClient, postgresDatabase, auditLogSyncJobMonitor, auditLogPublisher)
        }
    }

    fun fetchAndLog(eventDate: LocalDate, entity: String = "All", offset: Int = 0, salesforceClient: SalesforceClient, postgresDatabase: PostgresDatabase, auditLogSyncJobMonitor: AuditLogSyncJobMonitor, auditLogPublisher: AuditLogPublisher): Int {
        var totalNumberOfLoggedRecords = 0
        var totalNumberOfApiCalls = 0
        try {
            auditLogSyncJobMonitor.verifyJobIsNotAlreadyRan(eventDate, entity)
            Metrics.clearUriEventsCounter()

            val entitiesInObjectsYaml = entitySelectionService.fetchEntitiesInObjectsYaml()
            val entitiesToBeLogged = entitySelectionService.fetchEntitiesToBeLogged(entity, entitiesInObjectsYaml)
            val uriEventsInSalesforce = salesforceClient.fetchUriEvents(eventDate)
            val filteredUriEvents = uriEventEntityFilterService.filterUriEventsWithEntitiesToBeLogged(
                entitiesToBeLogged,
                uriEventsInSalesforce,

            )
            log.info { "Filtered ${filteredUriEvents.size} URI events" }
            filteredUriEvents.groupBy { it.entity }.forEach { (entity, events) ->
                // Get person idents for each recordId
                val personIdentsResponse = salesforceClient.fetchPersonIdents(
                    objectName = entity,
                    personIdentSelectClause = entitiesToBeLogged[entity] ?: "",
                    recordIds = events.map { it.recordId }
                )
                totalNumberOfApiCalls += personIdentsResponse.numberOfApiCalls

                // Publish Audit logs
                val uriEventsSummary = auditLogPublisher.publishLogs(events, personIdentsResponse, offset)

                totalNumberOfLoggedRecords += uriEventsSummary.uriEventsWithPersonIdentInt

                if (uriEventsSummary.uriEventsWithPersonIdent > 0) {
                    Metrics.uriEventsWithPersonIdent.labels(entity).inc(uriEventsSummary.uriEventsWithPersonIdent)
                    log.info() { "Logging ${uriEventsSummary.uriEventsWithPersonIdentInt} metrics entity $entity" }
                    postgresDatabase.insertAuditLogSyncStatus(
                        eventDate,
                        LocalDate.now(),
                        entity,
                        uriEventsSummary.uriEventsWithPersonIdentInt
                    )
                }
                if (uriEventsSummary.uriEventsWithoutAnyPersonIdents > 0) {
                    Metrics.uriEventsWithoutAnyPersonIdents.labels(entity).inc(uriEventsSummary.uriEventsWithoutAnyPersonIdents)
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
        return totalNumberOfLoggedRecords
    }
}
