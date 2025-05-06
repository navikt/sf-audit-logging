package no.nav.sf.audit.logging

import no.nav.sf.audit.logging.Metrics.numberOfApiCalls
import no.nav.sf.audit.logging.salesforce.DefaultSalesforceClient
import no.nav.sf.audit.logging.salesforce.PersonIdentsResponse
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import no.nav.sf.audit.logging.salesforce.UriEvent
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Response.Companion.invoke
import org.http4k.core.Status.Companion.OK

class AuditLogHandler(private val salesforceClient: SalesforceClient = DefaultSalesforceClient()) {
    private val objectFilter = ObjectFilter()
    private val uriEventsWithPersonIdent: MutableList<UriEvent> = mutableListOf()
    private val uriEventsWithoutAnyPersonIdents: MutableList<UriEvent> = mutableListOf()

    val fetchAndTransfer: HttpHandler = {
        Metrics.clearUriEventsCounter()
        val filteredUriEvents = objectFilter.filterUriEventsToHaveObjectsToBeLogged(salesforceClient.fetchUriEvents())
        var totalNumberOfLoggedRecords = 0
        var totalNumberOfApiCalls = 0

        filteredUriEvents.groupBy { it.entity }.forEach { (entity, events) ->
            val personIdentsResponse = salesforceClient.fetchPersonIdents(
                objectName = entity,
                personIdentSelectClause = objectFilter.objectsToBeLogged.getProperty(entity),
                recordIds = events.map { it.recordId }
            )
            totalNumberOfApiCalls += personIdentsResponse.numberOfApiCalls
            setUriEventsWithAndWithoutPersonIdent(events, personIdentsResponse)
            totalNumberOfLoggedRecords += uriEventsWithPersonIdent.size

            if (uriEventsWithPersonIdent.isNotEmpty()) {
                Metrics.uriEventsWithPersonIdent.labels(entity).inc(uriEventsWithPersonIdent.size.toDouble())
            }
            if (uriEventsWithoutAnyPersonIdents.isNotEmpty()) {
                Metrics.uriEventsWithoutAnyPersonIdents.labels(entity).inc(uriEventsWithoutAnyPersonIdents.size.toDouble())
            }
        }
        Metrics.numberOfApiCalls.labels("RequestPersonIdents").inc(totalNumberOfApiCalls.toDouble())
        Response(OK).body(Body(totalNumberOfLoggedRecords.toString()))
    }

    private fun setUriEventsWithAndWithoutPersonIdent(events: List<UriEvent>, personIdentsResponse: PersonIdentsResponse) {
        events.forEach { event ->
            val personIdent = personIdentsResponse.personIdentByRecordId[event.recordId]
            if (personIdent != null) {
                event.personIdent = personIdent
                uriEventsWithPersonIdent.add(event)
            } else {
                uriEventsWithoutAnyPersonIdents.add(event)
            }
        }
    }
}
