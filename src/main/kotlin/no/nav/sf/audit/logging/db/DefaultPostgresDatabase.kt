package no.nav.sf.audit.logging.db
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.sf.audit.logging.Application
import no.nav.sf.audit.logging.env
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDate

const val NAIS_DB_PREFIX = "NAIS_DATABASE_SF_AUDIT_LOGGING_SF_AUDIT_LOGGING_"

class DefaultPostgresDatabase : PostgresDatabase {

    val database = Database.connect(HikariDataSource(hikariConfig()))

    private val dbJdbcUrl = env("$NAIS_DB_PREFIX${Application.context}_JDBC_URL")
    private fun hikariConfig(): HikariConfig = HikariConfig().apply {
        jdbcUrl = dbJdbcUrl
        driverClassName = "org.postgresql.Driver"
        minimumIdle = 1
        maxLifetime = 26000
        maximumPoolSize = 10
        connectionTimeout = 250
        idleTimeout = 10000
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ" // Isolation level that ensure the same snapshot of db during one transaction
    }

    override fun retrieveAuditLogStatusesAsMap(): MutableMap<LocalDate, MutableMap<Boolean, Int>> {
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

    override fun upsertAuditLogStatus(syncDate: LocalDate, success: Boolean, numberOfRecords: Int): AuditLogStatus? {
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

    override fun fetchAuditLogStatus(syncDate: LocalDate, success: Boolean): List<AuditLogStatus> {
        return emptyList()
    }
}
