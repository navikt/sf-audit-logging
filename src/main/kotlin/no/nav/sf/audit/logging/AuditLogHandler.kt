package no.nav.sf.audit.logging

import no.nav.sf.audit.logging.Metrics.numberOfApiCalls
import no.nav.sf.audit.logging.db.PostgresDatabase
import no.nav.sf.audit.logging.salesforce.PersonIdentsResponse
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import no.nav.sf.audit.logging.salesforce.UriEvent
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Response.Companion.invoke
import org.http4k.core.Status.Companion.OK
import java.time.LocalDate

class AuditLogHandler(private val salesforceClient: SalesforceClient, postgresDatabase: PostgresDatabase) {
    private val objectFilter = ObjectFilter()

    val fetchAndTransfer: HttpHandler = fetchAndTransfer@{
        var totalNumberOfLoggedRecords = 0.0
        var totalNumberOfApiCalls = 0

        val successfulTransferSameDay = postgresDatabase.fetchAuditLogSyncStatus(LocalDate.now(), true)
        if (successfulTransferSameDay.isNotEmpty()) {
            // Stop if we have already transferred today
            return@fetchAndTransfer Response(OK).body(Body(totalNumberOfLoggedRecords.toString()))
        }
        Metrics.clearUriEventsCounter()
        val filteredUriEvents = objectFilter.filterUriEventsToHaveObjectsToBeLogged(salesforceClient.fetchUriEvents())

        filteredUriEvents.groupBy { it.entity }.forEach { (entity, events) ->
            val personIdentsResponse = salesforceClient.fetchPersonIdents(
                objectName = entity,
                personIdentSelectClause = objectFilter.objectsToBeLogged.getProperty(entity),
                recordIds = events.map { it.recordId }
            )
            totalNumberOfApiCalls += personIdentsResponse.numberOfApiCalls
            val (uriEventsWithPersonIdent, uriEventsWithoutAnyPersonIdents) = setUriEventsWithAndWithoutPersonIdent(events, personIdentsResponse)
            totalNumberOfLoggedRecords += uriEventsWithPersonIdent

            if (uriEventsWithPersonIdent> 0) {
                Metrics.uriEventsWithPersonIdent.labels(entity).inc(uriEventsWithPersonIdent)
            }
            if (uriEventsWithoutAnyPersonIdents> 0) {
                Metrics.uriEventsWithoutAnyPersonIdents.labels(entity).inc(uriEventsWithoutAnyPersonIdents)
            }
        }
        postgresDatabase.upsertAuditLogSyncStatus(LocalDate.now().minusDays(1), LocalDate.now(), true, totalNumberOfLoggedRecords.toInt())
        Metrics.numberOfApiCalls.labels("RequestPersonIdents").inc(totalNumberOfApiCalls.toDouble())
        Response(OK).body(Body(totalNumberOfLoggedRecords.toString()))
    }

    private fun setUriEventsWithAndWithoutPersonIdent(events: List<UriEvent>, personIdentsResponse: PersonIdentsResponse): Pair<Double, Double> {
        var uriEventsWithPersonIdent = 0.0
        var uriEventsWithoutAnyPersonIdents = 0.0
        events.forEach { event ->
            val personIdent = personIdentsResponse.personIdentByRecordId[event.recordId]
            if (personIdent != null) {
                event.personIdent = personIdent
                uriEventsWithPersonIdent += 1.0
            } else {
                uriEventsWithoutAnyPersonIdents += 1.0
            }
        }
        return Pair(uriEventsWithPersonIdent, uriEventsWithoutAnyPersonIdents)
    }
}
