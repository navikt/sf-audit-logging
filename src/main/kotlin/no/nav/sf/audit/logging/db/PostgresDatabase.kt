package no.nav.sf.audit.logging.db

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDate
import kotlin.collections.set

object PostgresDatabase {

    val auditLogStatusMap: MutableMap<LocalDate, MutableMap<Boolean, Int>> get() {
        return retrieveAuditLogStatusesAsMap()
    }

    private fun retrieveAuditLogStatusesAsMap(): MutableMap<LocalDate, MutableMap<Boolean, Int>> {
        return transaction {
            AuditLogStatusTable.selectAll()
                .map { it.toAuditLogStatus() }
                .groupBy { it.syncDate }
                .mapValues { entry ->
                    entry.value.groupBy { it.success }
                        .mapValues { innerEntry -> innerEntry.value.sumOf { it.numberOfRecords } }
                        .toMutableMap()
                }.toMutableMap()
        }
    }

    fun upsertAuditLogStatus(syncDate: LocalDate, success: Boolean, numberOfRecords: Int): AuditLogStatus? {
        return transaction {
            AuditLogStatusTable.upsert(
                keys = arrayOf(AuditLogStatusTable.syncDate, AuditLogStatusTable.success)
            ) {
                it[AuditLogStatusTable.syncDate] = syncDate
                it[AuditLogStatusTable.success] = success
                it[AuditLogStatusTable.numberOfRecords] = numberOfRecords
            }
        }.resultedValues?.firstOrNull()?.toAuditLogStatus()
    }
}
