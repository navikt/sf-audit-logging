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
    val entity: String,
    val numberOfRecords: Int
)

object AuditLogSyncStatusTable : Table("audit_log_status") {
    val eventDate = date("event_date")
    val syncDate = date("sync_date")
    val entity = varchar("salesforce_object", 43)
    val numberOfRecords = integer("number_of_records")

    init {
        uniqueIndex(eventDate, entity) // Enforces unique combinations of eventDate and Salesforce Object
    }
}

fun ResultRow.toAuditLogSyncStatus() = AuditLogSyncStatus(
    eventDate = this[AuditLogSyncStatusTable.eventDate],
    syncDate = this[AuditLogSyncStatusTable.syncDate],
    entity = this[AuditLogSyncStatusTable.entity],
    numberOfRecords = this[AuditLogSyncStatusTable.numberOfRecords]
)
fun getMetaData(postgresDatabase: PostgresDatabase = if (local) MockPostgresDatabase() else DefaultPostgresDatabase()): String {
    val auditLogSyncStatuses = postgresDatabase.retrieveAuditLogSyncStatusesAsMap()
    val now = LocalDateTime.now()
    val last30Days = now.minusDays(30).toLocalDate()

    val filteredAuditLogSyncStatuses = auditLogSyncStatuses.filterKeys { it.isAfter(last30Days) }
    return Application.gson.toJson(filteredAuditLogSyncStatuses.toSortedMap(compareByDescending { it }))
}
