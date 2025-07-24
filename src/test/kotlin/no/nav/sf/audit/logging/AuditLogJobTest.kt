package no.nav.sf.audit.logging

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.prometheus.client.Counter
import no.nav.sf.audit.logging.db.PostgresDatabase
import no.nav.sf.audit.logging.plugins.Metrics
import no.nav.sf.audit.logging.model.PersonIdentsResponse
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import no.nav.sf.audit.logging.model.UriEvent
import no.nav.sf.audit.logging.services.AuditLogSyncJobMonitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AuditLogJobTest {

    private val salesforceClient: SalesforceClient = mockk<SalesforceClient>()
    private val postgresDatabase = mockk<PostgresDatabase>()
    private val auditLogSyncJobMonitor = mockk<AuditLogSyncJobMonitor>()

    @BeforeEach
    fun setup() {
        mockkObject(Metrics)
        every { postgresDatabase.insertAuditLogSyncStatus(any(), any(), any(), any()) } returns true
        every { postgresDatabase.closeConnection() } returns Unit
        every { auditLogSyncJobMonitor.verifyJobIsNotAlreadyRan(any(), any()) } returns Unit
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

        val result = AuditLogJob.fetchAndLog(LocalDate.now(), "All", 0, salesforceClient, postgresDatabase, auditLogSyncJobMonitor)
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

        val result = AuditLogJob.fetchAndLog(LocalDate.now(), "All", 0, salesforceClient, postgresDatabase, auditLogSyncJobMonitor)
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

        val result = AuditLogJob.fetchAndLog(LocalDate.now(), "All", 0, salesforceClient, postgresDatabase, auditLogSyncJobMonitor)
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

        val result = AuditLogJob.fetchAndLog(LocalDate.now(), "All", 0, salesforceClient, postgresDatabase, auditLogSyncJobMonitor)
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

        AuditLogJob.fetchAndLog(LocalDate.now(), "All", 0, salesforceClient, postgresDatabase, auditLogSyncJobMonitor)
        verify(exactly = 1) { postgresDatabase.insertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "Account", 2) }
    }

    @Test
    fun `Should create one audit log records in postgres when uri events for account and case due to cas is excluded when fetching all entities`() {
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

        AuditLogJob.fetchAndLog(LocalDate.now(), "All", 0, salesforceClient, postgresDatabase, auditLogSyncJobMonitor)
        verify(exactly = 1) { postgresDatabase.insertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "Account", 1) }
        verify(exactly = 0) { postgresDatabase.insertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "Case", 1) }
    }

    @Test
    fun `Should create two audit log records in postgres when uri events for account and work order`() {
        val uriEvents = mutableListOf<UriEvent>()
        uriEvents.add(TestDataFactory.getUriEvent(1, "Account"))
        uriEvents.add(TestDataFactory.getUriEvent(2, "WorkOrder"))

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("WorkOrder") } returns mockCounterChild
        every { Metrics.numberOfApiCalls.labels("RequestPersonIdents") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901",
            "2" to "12345678902"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        AuditLogJob.fetchAndLog(LocalDate.now(), "All", 0, salesforceClient, postgresDatabase, auditLogSyncJobMonitor)
        verify(exactly = 1) { postgresDatabase.insertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "Account", 1) }
        verify(exactly = 1) { postgresDatabase.insertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "WorkOrder", 1) }
    }

    @Test
    fun `Should log one record when offset is 2 of 3`() {
        val uriEvents = TestDataFactory.getUriEvents(3)

        val mockCounterChild = mockk<Counter.Child>(relaxed = true)
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild
        every { Metrics.numberOfApiCalls.labels("RequestPersonIdents") } returns mockCounterChild

        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEvents)

        val personIdentByRecordId = mapOf(
            "1" to "12345678901",
            "2" to "12345678902",
            "3" to "12345678903"
        )
        val personIdentResponse = PersonIdentsResponse("Account", 1, personIdentByRecordId)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) }.returns(personIdentResponse)

        AuditLogJob.fetchAndLog(LocalDate.now(), "All", 2, salesforceClient, postgresDatabase, auditLogSyncJobMonitor)
        verify(exactly = 1) { postgresDatabase.insertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "Account", 1) }
    }
}
