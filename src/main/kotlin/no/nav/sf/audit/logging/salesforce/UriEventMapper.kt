package no.nav.sf.audit.logging.salesforce

import com.google.gson.JsonArray
import mu.KotlinLogging

class UriEventMapper {
    private val log = KotlinLogging.logger { }

    fun mapFromJsonArray(recordEntries: JsonArray): MutableList<UriEvent> {
        val result = mutableListOf<UriEvent>()
        try {
            log.info("Record entries $recordEntries")

            result.addAll(
                recordEntries.mapNotNull {
                    val record = it.asJsonObject
                    val eventDate = record["EventDate"]?.takeIf { !it.isJsonNull }?.toString()
                    val queriedEntities = record["QueriedEntities"]?.takeIf { !it.isJsonNull }?.toString()
                    val recordId = record["RecordId"]?.takeIf { !it.isJsonNull }?.toString()
                    val operation = record["Operation"]?.takeIf { !it.isJsonNull }?.toString()
                    val username = record["Username"]?.takeIf { !it.isJsonNull }?.toString()
                    if (eventDate != null && queriedEntities != null && recordId != null && operation != null && username != null) {
                        UriEvent(eventDate, queriedEntities, recordId, operation, username)
                    } else {
                        null
                    }
                }
            )
        } catch (e: Exception) {
            log.error("Failed to map JSON array to UriEvent: ${e.message}", e)
        }
        return result
    }
}
