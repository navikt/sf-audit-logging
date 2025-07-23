package no.nav.sf.audit.logging.db

import java.time.LocalDate

class MockPostgresDatabase : PostgresDatabase {
    override fun retrieveAuditLogSyncStatusesAsMap(): MutableMap<LocalDate, List<AuditLogSyncStatus>> {
        val auditLogSyncStatusesList = listOf(
            AuditLogSyncStatus(LocalDate.now().minusDays(1), LocalDate.now(), "Account", 10),
            AuditLogSyncStatus(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), "Account", 45),
            AuditLogSyncStatus(LocalDate.now().minusDays(3), LocalDate.now().minusDays(2), "Account", 20)
        )

        return auditLogSyncStatusesList.groupBy { it.syncDate }
            .mapValues { entry -> entry.value }
            .toMutableMap()
    }

    override fun insertAuditLogSyncStatus(eventDate: LocalDate, syncDate: LocalDate, entity: String, numberOfRecords: Int): Boolean {
        return true
    }

    override fun fetchAuditLogSyncStatus(eventDate: LocalDate): List<AuditLogSyncStatus> {
        return listOf(
            AuditLogSyncStatus(eventDate, LocalDate.now(), "Account", 10)
        )
    }

    override fun createStatusTable(dropFirst: Boolean) {
    override fun closeConnection() {
        // No operation for mock database
    }
}
