package no.nav.sf.audit.logging.db

import java.time.LocalDate

interface PostgresDatabase {
    fun retrieveAuditLogStatusesAsMap(): MutableMap<LocalDate, MutableMap<Boolean, Int>>

    fun upsertAuditLogStatus(syncDate: LocalDate, success: Boolean, numberOfRecords: Int): AuditLogStatus?

    fun fetchAuditLogStatus(syncDate: LocalDate, success: Boolean): List<AuditLogStatus>
}
