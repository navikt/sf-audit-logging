package no.nav.sf.audit.logging

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
    private val uriEventsWithoutPersonIdent: MutableList<UriEvent> = mutableListOf()

    val fetchAndTransfer: HttpHandler = {
        Metrics.clearUriEventsCounter()
        val filteredUriEvents = objectFilter.filterUriEventsToHaveObjectsToBeLogged(salesforceClient.fetchUriEvents())
        var totalNumberOfLoggedRecords = 0

        filteredUriEvents.groupBy { it.entity }.forEach { (entity, events) ->
            val personIdentsResponse = salesforceClient.fetchPersonIdents(
                objectName = entity,
                personIdentSelectClause = objectFilter.objectsToBeLogged.getProperty(entity),
                recordIds = events.map { it.recordId }
            )
            setUriEventsWithAndWithoutPersonIdent(events, personIdentsResponse)
            totalNumberOfLoggedRecords += uriEventsWithPersonIdent.size
            Metrics.uriEvents.labels(entity).inc(uriEventsWithPersonIdent.size.toDouble())
        }

        Response(OK).body(Body(totalNumberOfLoggedRecords.toString()))
    }

    private fun setUriEventsWithAndWithoutPersonIdent(events: List<UriEvent>, personIdentsResponse: PersonIdentsResponse) {
        events.forEach { event ->
            val personIdent = personIdentsResponse.personIdentByRecordId[event.recordId]
            if (personIdent != null) {
                event.personIdent = personIdent
                uriEventsWithPersonIdent.add(event)
            } else {
                uriEventsWithoutPersonIdent.add(event)
            }
        }
    }
}
