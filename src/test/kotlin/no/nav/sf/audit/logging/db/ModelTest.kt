package no.nav.sf.audit.logging.db

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ModelTest {
    private val postgresDatabase = mockk<PostgresDatabase>()
    private val auditLogSyncStatusesList = listOf(
        AuditLogSyncStatus(LocalDate.now().minusDays(1), LocalDate.now(), 45, true),
        AuditLogSyncStatus(LocalDate.now().minusDays(1), LocalDate.now().minusDays(1), 5, false),
    )

    @BeforeEach
    fun setup() {
        every {
            postgresDatabase.retrieveAuditLogSyncStatusesAsMap()
        }.returns(mutableMapOf(LocalDate.now() to auditLogSyncStatusesList))
    }

    @Test
    fun `Should get metadata in JSON format`() {
        val metadata = getMetaData(postgresDatabase)

        assertTrue(metadata.contains("\"numberOfRecords\": 45") && metadata.contains("\"success\": true"))
        assertTrue(metadata.contains("\"numberOfRecords\": 5") && metadata.contains("\"success\": false"))
    }
}
