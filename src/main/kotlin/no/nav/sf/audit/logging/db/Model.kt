package no.nav.sf.audit.logging.db

import no.nav.sf.audit.logging.Application
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.groupBy

data class AuditLogStatus(
    val syncDate: LocalDate,
    val numberOfRecords: Int,
    val success: Boolean
)

object AuditLogStatusTable : Table("audit_log_status") {
    val syncDate = date("sync_date")
    val numberOfRecords = integer("number_of_records")
    val success = bool("success")

    init {
        uniqueIndex(syncDate, success)
    }
}

fun ResultRow.toAuditLogStatus() = AuditLogStatus(
    syncDate = this[AuditLogStatusTable.syncDate],
    numberOfRecords = this[AuditLogStatusTable.numberOfRecords],
    success = this[AuditLogStatusTable.success]
)
fun getMetaData(): String {
    val auditLogStatuses = retrieveAuditLogStatusesAsMapMock()
    val now = LocalDateTime.now()
    val last30Days = now.minusDays(30).toLocalDate()

    val filteredAuditLogStatuses = auditLogStatuses.filterKeys { it.isAfter(last30Days) }
    return Application.gson.toJson(filteredAuditLogStatuses.toSortedMap(compareByDescending { it }))
}

fun retrieveAuditLogStatusesAsMapMock(): MutableMap<LocalDate, MutableMap<Boolean, Int>> {
    val auditLogStatusesList = listOf(
        AuditLogStatus(LocalDate.now(), 10, true),
        AuditLogStatus(LocalDate.now().minusDays(1), 5, false),
        AuditLogStatus(LocalDate.now().minusDays(2), 20, true),
    )

    return auditLogStatusesList.groupBy { it.syncDate }.mapValues { entry ->
        entry.value.groupBy { it.success }
            .mapValues { innerEntry ->
                innerEntry.value.sumOf { it.numberOfRecords }
            }
            .toMutableMap()
    }.toMutableMap()
}
