package no.nav.sf.audit.logging.db

import no.nav.sf.audit.logging.Application
import no.nav.sf.audit.logging.local
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate
import java.time.LocalDateTime

data class AuditLogSyncStatus(
    val eventDate: LocalDate,
    val syncDate: LocalDate,
    val numberOfRecords: Int,
    val success: Boolean
)

object AuditLogSyncStatusTable : Table("audit_log_status") {
    val eventDate = date("log_date")
    val syncDate = date("sync_date")
    val numberOfRecords = integer("number_of_records")
    val success = bool("success")

    init {
        uniqueIndex(eventDate)
    }
}

fun ResultRow.toAuditLogSyncStatus() = AuditLogSyncStatus(
    eventDate = this[AuditLogSyncStatusTable.eventDate],
    syncDate = this[AuditLogSyncStatusTable.syncDate],
    numberOfRecords = this[AuditLogSyncStatusTable.numberOfRecords],
    success = this[AuditLogSyncStatusTable.success]
)
fun getMetaData(postgresDatabase: PostgresDatabase = if (local) MockPostgresDatabase() else DefaultPostgresDatabase()): String {
    val AuditLogSyncStatuses = postgresDatabase.retrieveAuditLogSyncStatusesAsMap()
    val now = LocalDateTime.now()
    val last30Days = now.minusDays(30).toLocalDate()

    val filteredAuditLogSyncStatuses = AuditLogSyncStatuses.filterKeys { it.isAfter(last30Days) }
    return Application.gson.toJson(filteredAuditLogSyncStatuses.toSortedMap(compareByDescending { it }))
}
