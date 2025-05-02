package no.nav.sf.audit.logging

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.prometheus.client.Counter
import no.nav.sf.audit.logging.salesforce.PersonIdentsResponse
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import no.nav.sf.audit.logging.salesforce.UriEvent
import org.http4k.core.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuditLogHandlerTest {

    private val salesforceClient: SalesforceClient = mockk<SalesforceClient>()
    private val classUnderTest = AuditLogHandler(salesforceClient)
    private val uriEvents = mutableListOf(
        UriEvent(
            eventDateString = "2023-10-01T12:00:00.000+0000",
            entity = "Account",
            recordId = "001ABC123",
            operation = "INSERT",
            username = "user1",
            userType = "Standard"
        ),
        UriEvent(
            eventDateString = "2023-10-02T12:00:00.000+0000",
            entity = "Account",
            recordId = "101ABXX24",
            operation = "INSERT",
            username = "user1",
            userType = "Standard"
        )
    )

    @Test
    fun `Should store two uri logs for account in Metrics`() {
        mockkObject(Metrics)

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEvents.labels("Account") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents() }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "001ABC123" to "12345678901",
            "101ABXX24" to "12345678902"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        val result: Response = classUnderTest.fetchAndTransfer(org.http4k.core.Request(org.http4k.core.Method.GET, "/"))
        assertEquals(uriEvents.size, result.bodyString().toInt())

        verify(exactly = 1) { Metrics.uriEvents.labels("Account") }
        verify(exactly = 1) { mockCounterChild.inc(uriEvents.size.toDouble()) }
    }

    @Test
    fun `Should store one uri log for account in Metrics when only one has a person ident`() {
        mockkObject(Metrics)

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEvents.labels("Account") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents() }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "001ABC123" to "12345678901"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        val result: Response = classUnderTest.fetchAndTransfer(org.http4k.core.Request(org.http4k.core.Method.GET, "/"))
        assertEquals(1, result.bodyString().toInt())

        verify(exactly = 1) { Metrics.uriEvents.labels("Account") }
        verify(exactly = 1) { mockCounterChild.inc(1.0) }
    }

    @Test
    fun `Should log both events when the events have same record ID`() {
        val uriEventsWithSameRecordId = mutableListOf(
            UriEvent(
                eventDateString = "2023-10-01T12:00:00.000+0000",
                entity = "Account",
                recordId = "001ABC123",
                operation = "INSERT",
                username = "user1",
                userType = "Standard"
            ),
            UriEvent(
                eventDateString = "2023-10-02T11:00:00.000+0000",
                entity = "Account",
                recordId = "001ABC123",
                operation = "INSERT",
                username = "user1",
                userType = "Standard"
            )
        )
        mockkObject(Metrics)

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEvents.labels("Account") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents() }.returns(uriEventsWithSameRecordId)

        val personIdentByRecordId = mapOf(
            "001ABC123" to "12345678901"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        val result: Response = classUnderTest.fetchAndTransfer(org.http4k.core.Request(org.http4k.core.Method.GET, "/"))
        assertEquals(2, result.bodyString().toInt())

        verify(exactly = 1) { Metrics.uriEvents.labels("Account") }
        verify(exactly = 1) { mockCounterChild.inc(2.0) }
    }
}
