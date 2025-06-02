package no.nav.sf.audit.logging.db

import java.time.LocalDate

interface PostgresDatabase {
    fun retrieveAuditLogSyncStatusesAsMap(): MutableMap<LocalDate, List<AuditLogSyncStatus>>

    fun upsertAuditLogSyncStatus(eventDate: LocalDate, syncDate: LocalDate, salesforceObject: String, numberOfRecords: Int): AuditLogSyncStatus?

    fun fetchAuditLogSyncStatus(eventDate: LocalDate): List<AuditLogSyncStatus>

    fun fetchAuditLogSyncStatusBySalesforceObject(eventDate: LocalDate, salesforceObject: String): List<AuditLogSyncStatus>
}
