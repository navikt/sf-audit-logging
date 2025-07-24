package no.nav.sf.audit.logging.monitors

import java.time.LocalDate

interface AuditLogSyncMonitor {
    fun verifyJobIsNotAlreadyRan(eventDate: LocalDate, entity: String)
}
