package no.nav.sf.audit.logging.salesforce

import com.google.gson.JsonParser
import mu.KotlinLogging
import no.nav.sf.audit.logging.Application
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
        log.info("SOQL Query: $soqlQuery")
        val encodedQuery = URLEncoder.encode(soqlQuery, "UTF-8")
        var done = false
        var nextRecordsUrl = "/services/data/$apiVersion/query?q=$encodedQuery"

        var result = mutableListOf<UriEvent>()

        while (!done) {
            val request = org.http4k.core.Request(Method.GET, accessTokenHandler.instanceUrl + nextRecordsUrl)
                .header("Authorization", "Bearer ${accessTokenHandler.accessToken}")
                .header("Accept", "application/json")
            try {

                val response = client(request)
                if (response.status.successful) {
                    val obj = JsonParser.parseString(response.bodyString()).asJsonObject
                    result = UriEventMapper().mapFromJsonArray(obj["records"].asJsonArray)
                    var totalSize = obj["totalSize"].asInt
                    done = obj["done"].asBoolean
                    log.info { "Fetched ${result.size} of $totalSize URI events" }
                    if (!done) nextRecordsUrl = obj["nextRecordsUrl"].asString
                } else {
                    log.error { "Failed to fetch URI events - response ${response.status.code}:${response.bodyString()}" }
                    done = true
                }
            } catch (e: Exception) {
                log.error { "Exception while fetching URI events: ${e.message}" }
                done = true
            }
        }

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

        distinctRecordIds.chunked(5000).forEach { currentRecordIdRange ->
            val endpointUrl = "/services/apexrest/audit-logging/person-idents"
            log.info("Fetching person idents for ${currentRecordIdRange.size} records in object $objectName with select clause $personIdentSelectClause")
            val request = org.http4k.core.Request(Method.POST, accessTokenHandler.instanceUrl + endpointUrl)
                .header("Authorization", "Bearer ${accessTokenHandler.accessToken}")
                .header("Accept", "application/json")
                .body(Application.gson.toJson(PersonIdentsRequest(objectName, personIdentSelectClause, currentRecordIdRange)))
            val response = client(request)
            if (response.status.successful) {
                val recordEntries = JsonParser.parseString(response.bodyString()).asJsonArray
                recordEntries.forEach {
                    val recordId = it.asJsonObject["recordId"].asString
                    val personIdent = it.asJsonObject["personIdent"]
                        ?.takeIf { !it.isJsonNull }?.asString.orEmpty()
                    if (personIdent.isNotEmpty()) {
                        personIdentByRecordId[recordId] = personIdent
                    }
                }
                numberOfApiCalls++
            } else {
                log.error { "Failed to fetch person idents - response ${response.status.code}:${response.bodyString()}" }
                return PersonIdentsResponse(objectName, 0, mapOf())
            }
        }

        return PersonIdentsResponse(objectName, numberOfApiCalls, personIdentByRecordId)
    }

    private fun dateRestrictionExtention(date: LocalDate) =
        " EventDate >= ${date}T00:00:00Z AND EventDate < ${date.plusDays(1)}T00:00:00Z"
}
