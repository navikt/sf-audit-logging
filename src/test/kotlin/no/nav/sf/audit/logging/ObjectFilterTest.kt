package no.nav.sf.audit.logging

import no.nav.sf.audit.logging.salesforce.UriEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObjectFilterTest {

    @Test
    fun `Should refer to the person ident field on Accounts`() {
        val classUnderTest = ObjectFilter("All")
        val result = classUnderTest.objectsToBeLogged.getProperty("Account")
        assertEquals("INT_PersonIdent__c", result)
    }

    @Test
    fun `Should be more than one object to be logged`() {
        val classUnderTest = ObjectFilter("All")
        val objectsToBeLogged = classUnderTest.objectsToBeLogged
        assertTrue(objectsToBeLogged.size > 1)
    }

    @Test
    fun `Should filter out uri event with entity not listed in objects yaml`() {
        val uriEvents = listOf(
            UriEvent(
                eventDate = "2023-10-01T12:00:00.000+0000",
                entity = "Account",
                recordId = "001ABC123",
                operation = "INSERT",
                username = "user1"
            ),
            UriEvent(
                eventDate = "2023-10-02T15:30:00.000+0000",
                entity = "TestObject",
                recordId = "500XYZ456",
                operation = "UPDATE",
                username = "user2"
            )
        )

        val classUnderTest = ObjectFilter("All")
        val filteredEvents = classUnderTest.filterUriEventsToHaveObjectsToBeLogged(uriEvents)
        assertEquals(1, filteredEvents.size)
    }
}
