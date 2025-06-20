package no.nav.sf.audit.logging

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.prometheus.client.Counter
import no.nav.sf.audit.logging.db.AuditLogSyncStatus
import no.nav.sf.audit.logging.db.PostgresDatabase
import no.nav.sf.audit.logging.salesforce.PersonIdentsResponse
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import no.nav.sf.audit.logging.salesforce.UriEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class AuditLogJobTest {

    private val salesforceClient: SalesforceClient = mockk<SalesforceClient>()
    private val postgresDatabase = mockk<PostgresDatabase>()

    @BeforeEach
    fun setup() {
        mockkObject(Metrics)
        every { postgresDatabase.upsertAuditLogSyncStatus(any(), any(), any(), any()) }.returns(null)
        every { postgresDatabase.fetchAuditLogSyncStatus(any()) }.returns(emptyList())
    }

    @Test
    fun `Should store two uri logs for account in Metrics`() {
        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithPersonIdent.labels("Account") } returns mockCounterChild

        val uriEvents = TestDataFactory.getUriEvents(2)
        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901",
            "2" to "12345678902"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        val result = AuditLogJob.fetchAndLog(LocalDate.now(), "All", salesforceClient, postgresDatabase)
        assertEquals(uriEvents.size, result)

        verify(exactly = 1) { Metrics.uriEventsWithPersonIdent.labels("Account") }
        verify(exactly = 1) { mockCounterChild.inc(uriEvents.size.toDouble()) }
    }

    @Test
    fun `Should store one uri log for account in Metrics when only one has a person ident`() {

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithPersonIdent.labels("Account") } returns mockCounterChild

        val uriEvents = TestDataFactory.getUriEvents(2)
        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        val result = AuditLogJob.fetchAndLog(LocalDate.now(), "All", salesforceClient, postgresDatabase)
        assertEquals(1, result)

        verify(exactly = 1) { Metrics.uriEventsWithPersonIdent.labels("Account") }
        verify(exactly = 1) { mockCounterChild.inc(1.0) }
    }

    @Test
    fun `Should log both events when the events have same record ID`() {
        var uriEventsWithSameRecordId = TestDataFactory.getUriEvents(1)
        uriEventsWithSameRecordId.add(TestDataFactory.getUriEvents(1).first())

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithPersonIdent.labels("Account") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEventsWithSameRecordId)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        val result = AuditLogJob.fetchAndLog(LocalDate.now(), "All", salesforceClient, postgresDatabase)
        assertEquals(2, result)

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

        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEvents)

        // no matching person idents
        val personIdentByRecordId = mapOf(
            "99" to "12345678901"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        val result = AuditLogJob.fetchAndLog(LocalDate.now(), "All", salesforceClient, postgresDatabase)
        assertEquals(0, result)

        verify(exactly = 1) { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") }
        verify { mockCounterChild.inc(1.0) }
    }

    @Test
    fun `Should create one audit log record in Postgres when two uri events for account`() {
        val uriEvents = TestDataFactory.getUriEvents(2)

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.numberOfApiCalls.labels("RequestPersonIdents") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901",
            "2" to "12345678902"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        AuditLogJob.fetchAndLog(LocalDate.now(), "All", salesforceClient, postgresDatabase)
        verify(exactly = 1) { postgresDatabase.upsertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "Account", 2) }
    }

    @Test
    fun `Should log nothing if audit logs already has been successfully logged on the same day`() {
        val uriEvents = TestDataFactory.getUriEvents(2)

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.numberOfApiCalls.labels("RequestPersonIdents") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEvents)
        every { postgresDatabase.fetchAuditLogSyncStatus(any()) }.returns(
            listOf(
                AuditLogSyncStatus(
                    eventDate = LocalDate.now().minusDays(1),
                    syncDate = LocalDate.now(),
                    entity = "Account",
                    numberOfRecords = 1
                )
            )
        )

        val personIdentByRecordId = mapOf(
            "1" to "12345678901",
            "2" to "12345678902"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        assertThrows<IllegalStateException> {
            AuditLogJob.fetchAndLog(LocalDate.now(), "All", salesforceClient, postgresDatabase)
        }
    }

    @Test
    fun `Should create one audit log record in Postgres for Account When Case has been logged on the same day`() {
        val uriEvents = TestDataFactory.getUriEvents(2)

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.numberOfApiCalls.labels("RequestPersonIdents") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901",
            "2" to "12345678902"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        AuditLogJob.fetchAndLog(LocalDate.now(), "Account", salesforceClient, postgresDatabase)
        verify(exactly = 1) { postgresDatabase.upsertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "Account", 2) }
    }

    @Test
    fun `Should create two audit log records in postgres when uri events for account and case`() {
        val uriEvents = mutableListOf<UriEvent>()
        uriEvents.add(TestDataFactory.getUriEvent(1, "Account"))
        uriEvents.add(TestDataFactory.getUriEvent(2, "Case"))

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Case") } returns mockCounterChild
        every { Metrics.numberOfApiCalls.labels("RequestPersonIdents") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901",
            "2" to "12345678902"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        AuditLogJob.fetchAndLog(LocalDate.now(), "All", salesforceClient, postgresDatabase)
        verify(exactly = 1) { postgresDatabase.upsertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "Account", 1) }
        verify(exactly = 1) { postgresDatabase.upsertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "Case", 1) }
    }
}
