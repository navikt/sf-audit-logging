package no.nav.sf.audit.logging.salesforce

import com.google.gson.JsonParser
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
import java.time.LocalDate

class DefaultSalesforceClient(
    private val accessTokenHandler: AccessTokenHandler = DefaultAccessTokenHandler(),
    private val client: HttpHandler = ApacheClient()
) : SalesforceClient {
    private val log = KotlinLogging.logger { }

    private val apiVersion = env(config_SALESFORCE_API_VERSION)

    override fun fetchUriEvents(eventDate: LocalDate): List<UriEvent> {
        val soqlQuery = "SELECT EventDate, Operation, QueriedEntities, RecordId, Username, UserType FROM LightningUriEvent WHERE " + dateRestrictionExtention(eventDate)
        log.info { "Fetching $soqlQuery" }
        val encodedQuery = URLEncoder.encode(soqlQuery, "UTF-8")
        var done = false
        var nextRecordsUrl = "/services/data/$apiVersion/query?q=$encodedQuery"

        var result = mutableListOf<UriEvent>()
        var totalSize = 0

        while (!done) {
            val request = org.http4k.core.Request(Method.GET, accessTokenHandler.instanceUrl + nextRecordsUrl)
                .header("Authorization", "Bearer ${accessTokenHandler.accessToken}")
                .header("Accept", "application/json")

            val response = client(request)

            val responseBody = response.bodyString().takeIf { it.isNotEmpty() } ?: "No body in response"
            log.info { "Response status " + response.status }
            log.info { "Response  body " + responseBody }
            if (response.status.successful) {
                val obj = JsonParser.parseString(response.bodyString()).asJsonObject
                val recordEntries = obj["records"].asJsonArray
                result.addAll(
                    recordEntries.map {
                        UriEvent(
                            record["EventDate"]?.takeIf { !it.isJsonNull }?.asString ?: "",
                            record["QueriedEntities"]?.takeIf { !it.isJsonNull }?.asString ?: "",
                            record["RecordId"]?.takeIf { !it.isJsonNull }?.asString ?: "",
                            record["Operation"]?.takeIf { !it.isJsonNull }?.asString ?: "",
                            record["Username"]?.takeIf { !it.isJsonNull }?.asString ?: ""
                        )
                    }
                )
                totalSize = obj["totalSize"].asInt
                done = obj["done"].asBoolean
                if (!done) nextRecordsUrl = obj["nextRecordsUrl"].asString
            } else {
                log.error { "Failed to fetch URI events - response ${response.status.code}:${response.bodyString()}" }
                done = true
            }
        }
        log.info { "Fetched ${result.size} of $totalSize URI events" }
        return result
    }

    override fun fetchPersonIdents(
        objectName: String,
        personIdentSelectClause: String,
        recordIds: List<String>
    ): PersonIdentsResponse {
        val distinctRecordIds = recordIds.distinct()
        val personIdentByRecordId = mutableMapOf<String, String>()
        var numberOfApiCalls = 0

        distinctRecordIds.chunked(2000).forEach { currentRecordIdRange ->
            numberOfApiCalls++
        }

        return PersonIdentsResponse(objectName, numberOfApiCalls, personIdentByRecordId)
    }

    private fun dateRestrictionExtention(date: LocalDate) =
        " EventDate >= ${date}T00:00:00Z AND EventDate < ${date.plusDays(1)}T00:00:00Z"
}
