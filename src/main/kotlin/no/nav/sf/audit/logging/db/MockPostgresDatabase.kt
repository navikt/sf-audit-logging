package no.nav.sf.audit.logging.db

import java.time.LocalDate

class MockPostgresDatabase : PostgresDatabase {
    override fun retrieveAuditLogSyncStatusesAsMap(): MutableMap<LocalDate, List<AuditLogSyncStatus>> {
        val auditLogSyncStatusesList = listOf(
            AuditLogSyncStatus(LocalDate.now().minusDays(1), LocalDate.now(), 10, true),
            AuditLogSyncStatus(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), 5, false),
            AuditLogSyncStatus(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), 45, true),
            AuditLogSyncStatus(LocalDate.now().minusDays(3), LocalDate.now().minusDays(2), 20, true),
        )

        return auditLogSyncStatusesList.groupBy { it.syncDate }
            .mapValues { entry -> entry.value }
            .toMutableMap()
    }

    override fun upsertAuditLogSyncStatus(eventDate: LocalDate, syncDate: LocalDate, success: Boolean, numberOfRecords: Int): AuditLogSyncStatus? {
        return null
    }

    override fun fetchAuditLogSyncStatus(eventDate: LocalDate, success: Boolean): List<AuditLogSyncStatus> {
        return listOf(
            AuditLogSyncStatus(eventDate, LocalDate.now(), 10, success)
        )
    }
}
