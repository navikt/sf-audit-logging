package no.nav.sf.audit.logging.db

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

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
}
