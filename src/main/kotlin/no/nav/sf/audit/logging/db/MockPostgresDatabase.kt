package no.nav.sf.audit.logging.db

import java.time.LocalDate

class MockPostgresDatabase : PostgresDatabase {
    override fun retrieveAuditLogStatusesAsMap(): MutableMap<LocalDate, List<AuditLogStatus>> {
        val auditLogStatusesList = listOf(
            AuditLogStatus(LocalDate.now().minusDays(1), LocalDate.now(), 10, true),
            AuditLogStatus(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), 5, false),
            AuditLogStatus(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), 45, true),
            AuditLogStatus(LocalDate.now().minusDays(3), LocalDate.now().minusDays(2), 20, true),
        )

        return auditLogStatusesList.groupBy { it.syncDate }
            .mapValues { entry -> entry.value }
            .toMutableMap()
    }

    override fun upsertAuditLogStatus(eventDate: LocalDate, syncDate: LocalDate, success: Boolean, numberOfRecords: Int): AuditLogStatus? {
        return null
    }

    override fun fetchAuditLogStatus(syncDate: LocalDate, success: Boolean): List<AuditLogStatus> {
        return listOf(
            AuditLogStatus(syncDate.minusDays(1), syncDate, 10, success)
        )
    }
}
