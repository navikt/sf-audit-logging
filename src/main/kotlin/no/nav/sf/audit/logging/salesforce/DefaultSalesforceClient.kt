package no.nav.sf.audit.logging.salesforce

import mu.KotlinLogging
import no.nav.sf.audit.logging.config_SALESFORCE_API_VERSION
import no.nav.sf.audit.logging.env
import no.nav.sf.audit.logging.token.AccessTokenHandler
import no.nav.sf.audit.logging.token.DefaultAccessTokenHandler
import org.http4k.client.ApacheClient
import org.http4k.client.ApacheClient.invoke
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import java.net.URLEncoder

class DefaultSalesforceClient(
    private val accessTokenHandler: AccessTokenHandler = DefaultAccessTokenHandler(),
    private val client: HttpHandler = ApacheClient()
) : SalesforceClient {
    private val log = KotlinLogging.logger { }

    private val apiVersion = env(config_SALESFORCE_API_VERSION)

    override fun fetchUriEvents(): MutableList<UriEvent> {
        val soqlQuery = "SELECT EventDate, Operation, QueriedEntities, RecordId, Username, UserType FROM LightningUriEvent WHERE EventDate=yesterday"
        val encodedQuery = URLEncoder.encode(soqlQuery, "UTF-8")
        var done = false
        var nextRecordsUrl = "/services/data/$apiVersion/query?q=$encodedQuery"

        var result = mutableListOf<UriEvent>()
        var totalSize = 0

        while (!done) {
            val request = org.http4k.core.Request(Method.GET, accessTokenHandler.instanceUrl + nextRecordsUrl)
                .header("Authorization", "Bearer ${accessTokenHandler.accessToken}")
                .header("Accept", "application/json")
            try {
            } catch (e: Exception) {
                log.error { "Exception while fetching URI events: ${e.message}" }
                done = true
            }
        }
        log.info { "Fetched ${result.size} of $totalSize URI events" }
        return result
    }
}
