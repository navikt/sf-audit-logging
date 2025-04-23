package no.nav.sf.audit.logging

import mu.KotlinLogging
import no.nav.sf.audit.logging.salesforce.DefaultSalesforceClient
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Response.Companion.invoke
import org.http4k.core.Status.Companion.OK
import org.slf4j.Logger

class AuditLogHandler(private val salesforceClient: SalesforceClient = DefaultSalesforceClient(), private val log: Logger = KotlinLogging.logger { }) {

    fun fetchAndTransfer(): HttpHandler {
        val uriEvents = salesforceClient.fetchUriEvents()
        return { Response(OK).body(Body(uriEvents.size.toString())) }
    }
}
