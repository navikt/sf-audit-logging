package no.nav.sf.audit.logging.db

import java.time.LocalDate

interface PostgresDatabase {
    fun retrieveAuditLogSyncStatusesAsMap(): MutableMap<LocalDate, List<AuditLogSyncStatus>>

    fun insertAuditLogSyncStatus(
        eventDate: LocalDate,
        syncDate: LocalDate,
        entity: String,
        numberOfRecords: Int,
    ): Boolean

    fun fetchAuditLogSyncStatus(eventDate: LocalDate): List<AuditLogSyncStatus>

    fun createStatusTable(dropFirst: Boolean)

    fun closeConnection()
}
