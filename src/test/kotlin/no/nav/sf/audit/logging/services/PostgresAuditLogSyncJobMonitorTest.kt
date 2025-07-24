package no.nav.sf.audit.logging.services

import io.mockk.every
import io.mockk.mockk
import no.nav.sf.audit.logging.db.AuditLogSyncStatus
import no.nav.sf.audit.logging.db.PostgresDatabase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class PostgresAuditLogSyncJobMonitorTest {
    private val postgresDatabase = mockk<PostgresDatabase>()
    private val classUnderTest = PostgresAuditLogSyncJobMonitor(postgresDatabase)

    @BeforeEach
    fun setup() {
        every { postgresDatabase.closeConnection() } returns Unit
    }

    @Test
    fun `Should throw an error if audit logs already has been logged on the same day`() {

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
        assertThrows<IllegalStateException> {
            classUnderTest.verifyJobIsNotAlreadyRan(LocalDate.now(), "All")
        }
    }

    @Test
    fun `Should not throw errors for entity Account when Case has been logged on the same day`() {
        every { postgresDatabase.fetchAuditLogSyncStatus(any()) }.returns(
            listOf(
                AuditLogSyncStatus(
                    eventDate = LocalDate.now().minusDays(1),
                    syncDate = LocalDate.now(),
                    entity = "Case",
                    numberOfRecords = 1
                )
            )
        )
        classUnderTest.verifyJobIsNotAlreadyRan(LocalDate.now(), "Account")
    }

    @Test
    fun `Should not throw an error when no audit log sync records are found for today`() {
        every { postgresDatabase.fetchAuditLogSyncStatus(any()) }.returns(
            emptyList()
        )
        classUnderTest.verifyJobIsNotAlreadyRan(LocalDate.now(), "All")
    }
}
