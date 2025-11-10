package no.nav.sf.audit.logging.services

import no.nav.sf.audit.logging.TestDataFactory
import no.nav.sf.audit.logging.model.PersonIdentsResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultAuditLogPublisherTest {
    private val classUnderTest = DefaultAuditLogPublisher()

    @Test
    fun `Should log two uri events of Account`() {
        val uriEvents = TestDataFactory.getUriEvents(2)

        val personIdentByRecordId =
            mapOf(
                "1" to "12345678901",
                "2" to "12345678902",
            )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)

        val result = classUnderTest.publishLogs(uriEvents, personIdentResponse, 0)
        assertEquals(2.0, result.uriEventsWithPersonIdent)
        assertEquals(0.0, result.uriEventsWithoutAnyPersonIdents)
    }

    @Test
    fun `Should log one uri events of Account when only one of two has a person ident`() {
        val uriEvents = TestDataFactory.getUriEvents(2)

        val personIdentByRecordId =
            mapOf(
                "1" to "12345678901",
            )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)

        val result = classUnderTest.publishLogs(uriEvents, personIdentResponse, 0)
        assertEquals(1.0, result.uriEventsWithPersonIdent)
        assertEquals(1.0, result.uriEventsWithoutAnyPersonIdents)
    }

    @Test
    fun `Should log both events when the events have same record ID`() {
        var uriEventsWithSameRecordId = TestDataFactory.getUriEvents(1)
        uriEventsWithSameRecordId.add(TestDataFactory.getUriEvents(1).first())

        val personIdentByRecordId =
            mapOf(
                "1" to "12345678901",
            )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)

        val result = classUnderTest.publishLogs(uriEventsWithSameRecordId, personIdentResponse, 0)
        assertEquals(2.0, result.uriEventsWithPersonIdent)
        assertEquals(0.0, result.uriEventsWithoutAnyPersonIdents)
    }

    @Test
    fun `Should return metric for uri events without any person ident`() {
        val uriEvents = TestDataFactory.getUriEvents(1)

        val personIdentByRecordId =
            mapOf(
                "99" to "12345678901",
            )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)

        val result = classUnderTest.publishLogs(uriEvents, personIdentResponse, 0)
        assertEquals(0.0, result.uriEventsWithPersonIdent)
        assertEquals(1.0, result.uriEventsWithoutAnyPersonIdents)
    }

    @Test
    fun `Should log one record when offset is 2 of 3`() {
        val uriEvents = TestDataFactory.getUriEvents(3)

        val personIdentByRecordId =
            mapOf(
                "1" to "12345678901",
                "2" to "12345678902",
                "3" to "12345678903",
            )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)

        val result = classUnderTest.publishLogs(uriEvents, personIdentResponse, 2)
        assertEquals(1.0, result.uriEventsWithPersonIdent)
        assertEquals(0.0, result.uriEventsWithoutAnyPersonIdents)
    }
}
