package no.nav.sf.audit.logging

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.prometheus.client.Counter
import no.nav.sf.audit.logging.db.PostgresDatabase
import no.nav.sf.audit.logging.model.PersonIdentsResponse
import no.nav.sf.audit.logging.model.UriEvent
import no.nav.sf.audit.logging.model.UriEventsSummary
import no.nav.sf.audit.logging.plugins.Metrics
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import no.nav.sf.audit.logging.services.AuditLogPublisher
import no.nav.sf.audit.logging.services.AuditLogSyncJobMonitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AuditLogJobTest {

    private val salesforceClient: SalesforceClient = mockk<SalesforceClient>()
    private val postgresDatabase = mockk<PostgresDatabase>()
    private val auditLogSyncJobMonitor = mockk<AuditLogSyncJobMonitor>()
    private val auditLogPublisher = mockk<AuditLogPublisher>()
    private val personIdentsResponse = PersonIdentsResponse("Account", 1, emptyMap())
    private val mockCounterChild = mockk<Counter.Child>(relaxed = true)
    private val uriEvents = TestDataFactory.getUriEvents(2)

    @BeforeEach
    fun setup() {
        mockkObject(Metrics)
        every { postgresDatabase.insertAuditLogSyncStatus(any(), any(), any(), any()) } returns true
        every { postgresDatabase.closeConnection() } returns Unit
        every { auditLogSyncJobMonitor.verifyJobIsNotAlreadyRan(any(), any()) } returns Unit
        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEvents)
        every { salesforceClient.fetchPersonIdents(any(), any(), any()) } returns(personIdentsResponse)
        every { Metrics.uriEventsWithPersonIdent.labels("Account") } returns mockCounterChild
    }

    @Test
    fun `Should store two uri logs for account in Metrics`() {
        val uriEventsSummary = UriEventsSummary(2.0, 0.0)
        every { auditLogPublisher.publishLogs(any(), any(), any()) } returns uriEventsSummary

        val result = AuditLogJob.fetchAndLog(LocalDate.now(), "All", 0, salesforceClient, postgresDatabase, auditLogSyncJobMonitor, auditLogPublisher)
        assertEquals(2, result)

        verify(exactly = 1) { Metrics.uriEventsWithPersonIdent.labels("Account") }
        verify(exactly = 1) { mockCounterChild.inc(uriEvents.size.toDouble()) }
    }

    @Test
    fun `Should not log metric for uri events without any person ident`() {
        every { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") } returns mockCounterChild

        val uriEventsSummary = UriEventsSummary(0.0, 1.0)
        every { auditLogPublisher.publishLogs(any(), any(), any()) } returns uriEventsSummary

        val result = AuditLogJob.fetchAndLog(LocalDate.now(), "All", 0, salesforceClient, postgresDatabase, auditLogSyncJobMonitor, auditLogPublisher)
        assertEquals(0, result)

        verify(exactly = 1) { Metrics.uriEventsWithoutAnyPersonIdents.labels("Account") }
        verify { mockCounterChild.inc(1.0) }
    }

    @Test
    fun `Should create one audit log record in Postgres when two uri events for Account`() {
        val uriEventsSummary = UriEventsSummary(2.0, 0.0)
        every { auditLogPublisher.publishLogs(any(), any(), any()) } returns uriEventsSummary

        AuditLogJob.fetchAndLog(LocalDate.now(), "All", 0, salesforceClient, postgresDatabase, auditLogSyncJobMonitor, auditLogPublisher)
        verify(exactly = 1) { postgresDatabase.insertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "Account", 2) }
    }

    @Test
    fun `Should create two audit log records in postgres when uri events for account and work order`() {
        val uriEvents = mutableListOf<UriEvent>()
        uriEvents.add(TestDataFactory.getUriEvent(1, "Account"))
        uriEvents.add(TestDataFactory.getUriEvent(2, "WorkOrder"))
        every { salesforceClient.fetchUriEvents(any()) }.returns(uriEvents)
        every { Metrics.uriEventsWithPersonIdent.labels("WorkOrder") } returns mockCounterChild
        val uriEventsSummary = UriEventsSummary(1.0, 0.0)
        every { auditLogPublisher.publishLogs(any(), any(), any()) } returns uriEventsSummary

        AuditLogJob.fetchAndLog(LocalDate.now(), "All", 0, salesforceClient, postgresDatabase, auditLogSyncJobMonitor, auditLogPublisher)
        verify(exactly = 1) { postgresDatabase.insertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "Account", 1) }
        verify(exactly = 1) { postgresDatabase.insertAuditLogSyncStatus(LocalDate.now(), LocalDate.now(), "WorkOrder", 1) }
    }
}
