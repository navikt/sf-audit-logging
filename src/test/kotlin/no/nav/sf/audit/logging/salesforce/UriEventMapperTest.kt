package no.nav.sf.audit.logging.salesforce

import com.google.gson.JsonParser
import no.nav.sf.audit.logging.services.UriEventMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UriEventMapperTest {
    private val classUnderTest = UriEventMapper()
    private val recordEntries =
        "[{\"attributes\":{\"type\":\"LightningUriEvent\",\"url\":\"/services/data/v62.0/sobjects/LightningUriEvent/000000000000000AAA\"}" +
            ",\"EventDate\":\"2025-06-05T13:22:23.076+0000\",\"Operation\":\"Read\",\"QueriedEntities\":\"NavTask__c\",\"RecordId\":\"a1Cds000001c7ntEAA\",\"Username\":\"testuser\",\"UserType\":\"Standard\"}]"
    private val recordEntriesWithMissingEntityValue =
        "[{\"attributes\":{\"type\":\"LightningUriEvent\"," +
            "\"url\":\"/services/data/v62.0/sobjects/LightningUriEvent/000000000000000AAA\"}," +
            "\"EventDate\":\"2025-06-05T13:22:23.076+0000\",\"Operation\":\"Read\",\"QueriedEntities\":\"\",\"RecordId\":\"a1Cds000001c7ntEAA\",\"Username\":\"testuser\",\"UserType\":\"Standard\"}]"

    @Test
    fun `Should return uri event with value for all properties`() {
        val recordEntriesArray = JsonParser.parseString(recordEntries).asJsonArray
        val result = classUnderTest.mapFromJsonArray(recordEntriesArray)
        assertEquals(1, result.size)
        assertEquals("NavTask__c", result[0].entity)
        assertEquals("2025-06-05T13:22:23.076+0000", result[0].eventDate)
        assertEquals("a1Cds000001c7ntEAA", result[0].recordId)
        assertEquals("Read", result[0].operation)
        assertEquals("testuser", result[0].username)
    }

    @Test
    fun `Should not map record when value is missing for entity`() {
        val recordEntriesArray = JsonParser.parseString(recordEntriesWithMissingEntityValue).asJsonArray
        val result = classUnderTest.mapFromJsonArray(recordEntriesArray)
        assertEquals(0, result.size)
    }
}
