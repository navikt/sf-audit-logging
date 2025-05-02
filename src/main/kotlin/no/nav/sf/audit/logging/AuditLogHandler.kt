package no.nav.sf.audit.logging

import no.nav.sf.audit.logging.salesforce.DefaultSalesforceClient
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Response.Companion.invoke
import org.http4k.core.Status.Companion.OK

class AuditLogHandler(private val salesforceClient: SalesforceClient = DefaultSalesforceClient()) {
    private val objectFilter = ObjectFilter()
    val fetchAndTransfer: HttpHandler = {
        Metrics.clearUriEventsCounter()
        val uriEvents = salesforceClient.fetchUriEvents()
        val filteredUriEvents = objectFilter.filterUriEventsToHaveObjectsToBeLogged(uriEvents)
        var totalNumberOfLoggedRecords = 0

        val groupedUriEvents = filteredUriEvents.groupBy { it.entity }
        for ((entity, events) in groupedUriEvents) {

            val personIdentsResponse = salesforceClient.fetchPersonIdents(
                objectName = entity,
                personIdentSelectClause = objectFilter.objectsToBeLogged.getProperty(entity),
                recordIds = events.map { it.recordId }
            )
            totalNumberOfLoggedRecords += personIdentsResponse.personIdentByRecordId.size
            Metrics.uriEvents.labels(entity).inc(personIdentsResponse.personIdentByRecordId.size.toDouble())
        }
        Response(OK).body(Body(totalNumberOfLoggedRecords.toString()))
    }
}
