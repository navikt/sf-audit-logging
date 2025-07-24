package no.nav.sf.audit.logging.monitors

import no.nav.sf.audit.logging.db.PostgresDatabase
import java.time.LocalDate

class DefaultAuditLogSyncMonitor(private val postgresDatabase: PostgresDatabase) : AuditLogSyncMonitor {
    override fun verifyJobIsNotAlreadyRan(eventDate: LocalDate, entity: String) {
        if (entity == "All" && postgresDatabase.fetchAuditLogSyncStatus(eventDate).isNotEmpty()) {
            throw IllegalStateException("Audit logs have already been logged for $eventDate")
        }
        postgresDatabase.closeConnection()
    }
}
