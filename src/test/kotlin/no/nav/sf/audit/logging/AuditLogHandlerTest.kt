package no.nav.sf.audit.logging

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.prometheus.client.Counter
import no.nav.sf.audit.logging.db.PostgresDatabase
import no.nav.sf.audit.logging.salesforce.PersonIdentsResponse
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import org.http4k.core.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AuditLogHandlerTest {

    private val salesforceClient: SalesforceClient = mockk<SalesforceClient>()
    private val postgresDatabase = mockk<PostgresDatabase>()
    private val classUnderTest = AuditLogHandler(salesforceClient, postgresDatabase)

    @BeforeEach
    fun setup() {
        mockkObject(Metrics)
        every { postgresDatabase.upsertAuditLogStatus(any(), any(), any()) }.returns(null)
    }

    @Test
    fun `Should store two uri logs for account in Metrics`() {
        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithPersonIdent.labels("Account") } returns mockCounterChild

        val uriEvents = TestDataFactory.getUriEvents(2)
        every { salesforceClient.fetchUriEvents() }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901",
            "2" to "12345678902"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        val result: Response = classUnderTest.fetchAndTransfer(org.http4k.core.Request(org.http4k.core.Method.GET, "/"))
        assertEquals(uriEvents.size.toDouble(), result.bodyString().toDouble())

        verify(exactly = 1) { Metrics.uriEventsWithPersonIdent.labels("Account") }
        verify(exactly = 1) { mockCounterChild.inc(uriEvents.size.toDouble()) }
    }

    @Test
    fun `Should store one uri log for account in Metrics when only one has a person ident`() {

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithPersonIdent.labels("Account") } returns mockCounterChild

        val uriEvents = TestDataFactory.getUriEvents(2)
        every { salesforceClient.fetchUriEvents() }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        val result: Response = classUnderTest.fetchAndTransfer(org.http4k.core.Request(org.http4k.core.Method.GET, "/"))
        assertEquals(1.0, result.bodyString().toDouble())

        verify(exactly = 1) { Metrics.uriEventsWithPersonIdent.labels("Account") }
        verify(exactly = 1) { mockCounterChild.inc(1.0) }
    }

    @Test
    fun `Should log both events when the events have same record ID`() {
        var uriEventsWithSameRecordId = TestDataFactory.getUriEvents(1)
        uriEventsWithSameRecordId.add(TestDataFactory.getUriEvents(1).first())

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithPersonIdent.labels("Account") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents() }.returns(uriEventsWithSameRecordId)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        val result: Response = classUnderTest.fetchAndTransfer(org.http4k.core.Request(org.http4k.core.Method.GET, "/"))
        assertEquals(2.0, result.bodyString().toDouble())

        verify(exactly = 1) { Metrics.uriEventsWithPersonIdent.labels("Account") }
        verify(exactly = 1) { mockCounterChild.inc(2.0) }
    }

    @Test
    fun `Should log metric for uri events without any person ident`() {
        val uriEvents = TestDataFactory.getUriEvents(1)

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.numberOfApiCalls.labels("RequestPersonIdents") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents() }.returns(uriEvents)

        // no matching person idents
        val personIdentByRecordId = mapOf(
            "99" to "12345678901"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        val result: Response = classUnderTest.fetchAndTransfer(org.http4k.core.Request(org.http4k.core.Method.GET, "/"))
        assertEquals(0.0, result.bodyString().toDouble())

        verify(exactly = 1) { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") }
        verify { mockCounterChild.inc(1.0) }
    }

    @Test
    fun `Should create one audit store record as succeded in postgres`() {
        val uriEvents = TestDataFactory.getUriEvents(2)

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.numberOfApiCalls.labels("RequestPersonIdents") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents() }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901",
            "2" to "12345678902"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        classUnderTest.fetchAndTransfer(org.http4k.core.Request(org.http4k.core.Method.GET, "/"))
        verify(exactly = 1) { postgresDatabase.upsertAuditLogStatus(LocalDate.now(), true, 2) }
    }
}
