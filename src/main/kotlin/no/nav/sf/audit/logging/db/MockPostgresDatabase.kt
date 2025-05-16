package no.nav.sf.audit.logging.db

import java.time.LocalDate

class MockPostgresDatabase : PostgresDatabase {
    override fun retrieveAuditLogStatusesAsMap(): MutableMap<LocalDate, MutableMap<Boolean, Int>> {
        val auditLogStatusesList = listOf(
            AuditLogStatus(LocalDate.now(), 10, true),
            AuditLogStatus(LocalDate.now().minusDays(1), 5, false),
            AuditLogStatus(LocalDate.now().minusDays(1), 45, true),
            AuditLogStatus(LocalDate.now().minusDays(2), 20, true),
        )

        return auditLogStatusesList.groupBy { it.syncDate }.mapValues { entry ->
            entry.value.groupBy { it.success }
                .mapValues { innerEntry ->
                    innerEntry.value.sumOf { it.numberOfRecords }
                }
                .toMutableMap()
        }.toMutableMap()
    }

    override fun upsertAuditLogStatus(syncDate: LocalDate, success: Boolean, numberOfRecords: Int): AuditLogStatus? {
        return null
    }
}
