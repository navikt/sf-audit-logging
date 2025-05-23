package no.nav.sf.audit.logging.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.sf.audit.logging.Application
import no.nav.sf.audit.logging.env
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDate

const val NAIS_DB_PREFIX = "NAIS_DATABASE_SF_AUDIT_LOGGING_SF_AUDIT_LOGGING_"

class DefaultPostgresDatabase : PostgresDatabase {
    private val log = KotlinLogging.logger { }
    private val dbJdbcUrl = env("${no.nav.sf.audit.logging.db.NAIS_DB_PREFIX}${Application.context}_JDBC_URL")
    val database = try {
        Database.connect(HikariDataSource(hikariConfig()))
    } catch (e: Exception) {
        log.error("Failed to connect to the database", e)
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
        return transaction(database) {
            AuditLogSyncStatusTable.selectAll()
                .map { it.toAuditLogSyncStatus() }
                .groupBy { it.syncDate }
                .toMutableMap()
        }
    }

    override fun upsertAuditLogSyncStatus(eventDate: LocalDate, syncDate: LocalDate, success: Boolean, numberOfRecords: Int): AuditLogSyncStatus? {
        log.info { "Store $eventDate $syncDate $success $numberOfRecords" }
        return transaction(database) {
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
        return transaction(database) {
            AuditLogSyncStatusTable.selectAll()
                .where {
                    (AuditLogSyncStatusTable.eventDate eq eventDate) and
                        (AuditLogSyncStatusTable.success eq success)
                }
                .map { it.toAuditLogSyncStatus() }
        }
    }

    fun createStatusTable(dropFirst: Boolean = false) {
        transaction {
            if (dropFirst) {
                log.info { "Dropping table log_sync_status" }
                val dropStatement =
                    TransactionManager.current().connection.prepareStatement("DROP TABLE audit_log_status", false)
                dropStatement.executeUpdate()
                log.info { "Drop performed" }
            }

            log.info { "Creating table audit_log_status" }
            SchemaUtils.create(AuditLogSyncStatusTable)
        }
    }
}
