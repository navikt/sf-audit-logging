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

                val response = client(request)
                if (response.status.successful) {
                    val obj = JsonParser.parseString(response.bodyString()).asJsonObject
                    val recordEntries = obj["records"].asJsonArray
                    result.addAll(
                        recordEntries.map {
                            UriEvent(
                                it.asJsonObject["EventDate"].asString,
                                it.asJsonObject["QueriedEntities"].asString,
                                it.asJsonObject["RecordId"].asString,
                                it.asJsonObject["Operation"].asString,
                                it.asJsonObject["Username"].asString,
                                it.asJsonObject["UserType"].asString
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
            } catch (e: Exception) {
                log.error { "Exception while fetching URI events: ${e.message}" }
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
        var personIdentByRecordId = mutableMapOf<String, String>()
        var numberOfRequests = 0
        var numberOfRecords = 0

        // fetch person idents in batches of 2000
        while (numberOfRecords < distinctRecordIds.size) {
            val currentRecordIdRange = distinctRecordIds.subList(numberOfRecords, minOf(numberOfRecords + 2000, distinctRecordIds.size))
            numberOfRecords += currentRecordIdRange.size
            val soqlQuery = "SELECT Id, $personIdentSelectClause FROM $objectName WHERE Id IN (${currentRecordIdRange.joinToString(",")})"
            val encodedQuery = URLEncoder.encode(soqlQuery, "UTF-8")
            var recordsUrl = "/services/data/$apiVersion/query?q=$encodedQuery"

            val request = org.http4k.core.Request(Method.GET, accessTokenHandler.instanceUrl + recordsUrl)
                .header("Authorization", "Bearer ${accessTokenHandler.accessToken}")
                .header("Accept", "application/json")
            val response = client(request)
            if (response.status.successful) {
                val obj = JsonParser.parseString(response.bodyString()).asJsonObject
                val recordEntries = obj["records"].asJsonArray
                recordEntries.forEach {
                    val recordId = it.asJsonObject["Id"].asString
                    val personIdent = it.asJsonObject["INT_PersonIdent__c"].takeIf { !it.isJsonNull }?.asString ?: ""
                    if (personIdent.isNotEmpty()) {
                        personIdentByRecordId[recordId] = personIdent
                    }
                }

                numberOfRequests++
            } else {
                log.error { "Failed to fetch person idents - response ${response.status.code}:${response.bodyString()}" }
                return PersonIdentsResponse(objectName, 0, mapOf())
            }
        }

        return PersonIdentsResponse(objectName, numberOfRequests, personIdentByRecordId)
    }
}
