package no.nav.sf.audit.logging

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.prometheus.client.Counter
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
    fun `Should store number of uri logs for account in Metrics`() {
        mockkObject(Metrics)

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEvents.labels("Account") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents() }.returns(uriEvents)
        val result: Response = classUnderTest.fetchAndTransfer(org.http4k.core.Request(org.http4k.core.Method.GET, "/"))
        assertEquals(uriEvents.size, result.bodyString().toInt())

        verify(exactly = 1) { Metrics.uriEvents.labels("Account") }
        verify(exactly = 1) { mockCounterChild.inc(uriEvents.size.toDouble()) }
    }
}
