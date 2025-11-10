package no.nav.sf.audit.logging.services

import no.nav.sf.audit.logging.db.PostgresDatabase
import java.time.LocalDate

class PostgresAuditLogSyncJobMonitor(
    private val postgresDatabase: PostgresDatabase,
) : AuditLogSyncJobMonitor {
    override fun verifyJobIsNotAlreadyRan(
        eventDate: LocalDate,
        entity: String,
    ) {
        if (entity == "All" && postgresDatabase.fetchAuditLogSyncStatus(eventDate).isNotEmpty()) {
            throw IllegalStateException("Audit logs have already been logged for $eventDate")
        }
        postgresDatabase.closeConnection()
    }
}
