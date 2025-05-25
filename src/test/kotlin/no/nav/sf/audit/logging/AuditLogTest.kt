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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AuditLogTest {

    private val salesforceClient: SalesforceClient = mockk<SalesforceClient>()
    private val postgresDatabase = mockk<PostgresDatabase>()
    private val classUnderTest = AuditLog(salesforceClient, postgresDatabase)

    @BeforeEach
    fun setup() {
        mockkObject(Metrics)
        every { postgresDatabase.upsertAuditLogSyncStatus(any(), any(), any()) }.returns(null)
        every { postgresDatabase.fetchAuditLogSyncStatus(any()) }.returns(emptyList())
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

        val result = classUnderTest.fetchAndLog(LocalDate.now())
        assertEquals(1, result)

        verify(exactly = 1) { Metrics.uriEventsWithPersonIdent.labels("Account") }
        verify(exactly = 1) { mockCounterChild.inc(1.0) }
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

        val result = classUnderTest.fetchAndLog(LocalDate.now())
        assertEquals(0, result)
    }
}
