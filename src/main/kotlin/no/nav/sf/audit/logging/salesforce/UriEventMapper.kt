package no.nav.sf.audit.logging.salesforce

import com.google.gson.JsonArray
import mu.KotlinLogging

class UriEventMapper {
    private val log = KotlinLogging.logger { }

    fun mapFromJsonArray(recordEntries: JsonArray): MutableList<UriEvent> {
        val result = mutableListOf<UriEvent>()
        result.addAll(
            recordEntries.mapNotNull {
                val record = it.asJsonObject
                val eventDate = record["EventDate"]?.takeIf { !it.isJsonNull }?.asString
                val queriedEntities = record["QueriedEntities"]?.takeIf { !it.isJsonNull }?.asString
                val recordId = record["RecordId"]?.takeIf { !it.isJsonNull }?.asString
                val operation = record["Operation"]?.takeIf { !it.isJsonNull }?.asString
                val username = record["Username"]?.takeIf { !it.isJsonNull }?.asString
                if (!eventDate.isNullOrBlank() && !queriedEntities.isNullOrBlank() && !recordId.isNullOrBlank() && !operation.isNullOrBlank() && !username.isNullOrBlank()) {
                    UriEvent(eventDate, queriedEntities, recordId, operation, username)
                } else {
                    null
                }
            }
        )
        return result
    }
}
