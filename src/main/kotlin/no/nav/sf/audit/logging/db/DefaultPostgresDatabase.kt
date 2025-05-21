package no.nav.sf.audit.logging.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.sf.audit.logging.Application
import no.nav.sf.audit.logging.env
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDate

const val NAIS_DB_PREFIX = "NAIS_DATABASE_SF_AUDIT_LOGGING_SF_AUDIT_LOGGING_"

class DefaultPostgresDatabase : PostgresDatabase {
    val log = KotlinLogging.logger { }

    val database = try {
        Database.connect(HikariDataSource(hikariConfig()))
    } catch (e: Exception) {
        log.error(e) { "Could not connect to database" }
        null
    }
    private val dbJdbcUrl = try {
        env("$NAIS_DB_PREFIX${Application.context}_JDBC_URL")
    } catch (e: Exception) {
        log.error(e) { "Could not retrieve JDBC URL from environment" }
        null
    }
    private fun hikariConfig(): HikariConfig = HikariConfig().apply {
        jdbcUrl = dbJdbcUrl
        driverClassName = "org.postgresql.Driver"
        minimumIdle = 1
        maxLifetime = 26000
        maximumPoolSize = 10
        connectionTimeout = 250
        idleTimeout = 10000
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    }

    override fun retrieveAuditLogSyncStatusesAsMap(): MutableMap<LocalDate, List<AuditLogSyncStatus>> {
        return transaction {
            AuditLogSyncStatusTable.selectAll()
                .map { it.toAuditLogSyncStatus() }
                .groupBy { it.syncDate }
                .toMutableMap()
        }
    }

    override fun upsertAuditLogSyncStatus(eventDate: LocalDate, syncDate: LocalDate, success: Boolean, numberOfRecords: Int): AuditLogSyncStatus? {
        return transaction {
            AuditLogSyncStatusTable.upsert(
                keys = arrayOf(AuditLogSyncStatusTable.syncDate, AuditLogSyncStatusTable.success)
            ) {
                it[AuditLogSyncStatusTable.eventDate] = eventDate
                it[AuditLogSyncStatusTable.syncDate] = syncDate
                it[AuditLogSyncStatusTable.success] = success
                it[AuditLogSyncStatusTable.numberOfRecords] = numberOfRecords
            }
        }.resultedValues?.firstOrNull()?.toAuditLogSyncStatus()
    }

    override fun fetchAuditLogSyncStatus(eventDate: LocalDate, success: Boolean): List<AuditLogSyncStatus> {
        return transaction {
            AuditLogSyncStatusTable.selectAll()
                .where {
                    (AuditLogSyncStatusTable.eventDate eq eventDate) and
                        (AuditLogSyncStatusTable.success eq success)
                }
                .map { it.toAuditLogSyncStatus() }
        }
    }
}
