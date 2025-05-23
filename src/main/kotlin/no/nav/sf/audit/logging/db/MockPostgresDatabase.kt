package no.nav.sf.audit.logging.db

import java.time.LocalDate

class MockPostgresDatabase : PostgresDatabase {
    override fun retrieveAuditLogSyncStatusesAsMap(): MutableMap<LocalDate, List<AuditLogSyncStatus>> {
        val auditLogSyncStatusesList = listOf(
            AuditLogSyncStatus(LocalDate.now().minusDays(1), LocalDate.now(), 10),
            AuditLogSyncStatus(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), 45),
            AuditLogSyncStatus(LocalDate.now().minusDays(3), LocalDate.now().minusDays(2), 20)
        )

        return auditLogSyncStatusesList.groupBy { it.syncDate }
            .mapValues { entry -> entry.value }
            .toMutableMap()
    }

    override fun upsertAuditLogSyncStatus(eventDate: LocalDate, syncDate: LocalDate, numberOfRecords: Int): AuditLogSyncStatus? {
        return null
    }

    override fun fetchAuditLogSyncStatus(eventDate: LocalDate): List<AuditLogSyncStatus> {
        return listOf(
            AuditLogSyncStatus(eventDate, LocalDate.now(), 10)
        )
    }
}
