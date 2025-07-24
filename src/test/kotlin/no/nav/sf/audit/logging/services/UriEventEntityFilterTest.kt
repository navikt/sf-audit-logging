package no.nav.sf.audit.logging.services

import no.nav.sf.audit.logging.model.UriEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UriEventEntityFilterTest {
    private val classUnderTest = UriEventEntityFilter()
    private val entities = mutableMapOf<String, String>()

    init {
        entities["Account"] = "INT_PersonIdent__c"
        entities["NavTask__c"] = "INT_PersonIdent__c"
        entities["Case"] = "INT_CaseIdent__c"
    }

    @Test
    fun `Should filter out uri event with entity not listed in objects yaml`() {
        val uriEvents = listOf(
            UriEvent(
                eventDate = "2023-10-01T12:00:00.000+0000",
                entity = "Account",
                recordId = "001ABC123",
                operation = "INSERT",
                username = "user1",
                userType = "Standard"
            ),
            UriEvent(
                eventDate = "2023-10-02T15:30:00.000+0000",
                entity = "TestObject",
                recordId = "500XYZ456",
                operation = "UPDATE",
                username = "user2",
                userType = "Standard"
            )
        )
        val filteredEvents = classUnderTest.filterUriEventsWithEntitiesToBeLogged(entities, uriEvents)
        assertEquals(1, filteredEvents.size)
    }

    @Test
    fun `Should log one record when one of two records have user type Standard`() {
        val uriEvents = listOf(
            UriEvent(
                eventDate = "2023-10-01T12:00:00.000+0000",
                entity = "Account",
                recordId = "001ABC123",
                operation = "INSERT",
                username = "user1",
                userType = "Standard"
            ),
            UriEvent(
                eventDate = "2023-10-02T15:30:00.000+0000",
                entity = "Account",
                recordId = "500XYZ456",
                operation = "UPDATE",
                username = "user2",
                userType = "Portal"
            )
        )

        val filteredEvents = classUnderTest.filterUriEventsWithEntitiesToBeLogged(entities, uriEvents)
        assertEquals(1, filteredEvents.size)
    }
}
