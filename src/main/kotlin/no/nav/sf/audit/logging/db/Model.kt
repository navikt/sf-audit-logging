package no.nav.sf.audit.logging.db

import no.nav.sf.audit.logging.Application
import no.nav.sf.audit.logging.local
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate
import java.time.LocalDateTime

data class AuditLogStatus(
    val eventDate: LocalDate,
    val syncDate: LocalDate,
    val numberOfRecords: Int,
    val success: Boolean
)

object AuditLogStatusTable : Table("audit_log_status") {
    val eventDate = date("log_date")
    val syncDate = date("sync_date")
    val numberOfRecords = integer("number_of_records")
    val success = bool("success")

    init {
        uniqueIndex(eventDate, success)
    }
}

private val postgresDatabase: PostgresDatabase = if (local) MockPostgresDatabase() else DefaultPostgresDatabase()

fun ResultRow.toAuditLogStatus() = AuditLogStatus(
    eventDate = this[AuditLogStatusTable.eventDate],
    syncDate = this[AuditLogStatusTable.syncDate],
    numberOfRecords = this[AuditLogStatusTable.numberOfRecords],
    success = this[AuditLogStatusTable.success]
)
fun getMetaData(): String {
    val auditLogStatuses = postgresDatabase.retrieveAuditLogStatusesAsMap()
    val now = LocalDateTime.now()
    val last30Days = now.minusDays(30).toLocalDate()

    val filteredAuditLogStatuses = auditLogStatuses.filterKeys { it.isAfter(last30Days) }
    return Application.gson.toJson(filteredAuditLogStatuses.toSortedMap(compareByDescending { it }))
}
