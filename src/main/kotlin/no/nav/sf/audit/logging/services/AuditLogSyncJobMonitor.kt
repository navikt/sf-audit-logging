package no.nav.sf.audit.logging.services

import java.time.LocalDate

interface AuditLogSyncJobMonitor {
    fun verifyJobIsNotAlreadyRan(
        eventDate: LocalDate,
        entity: String,
    )
}
