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

    private fun hikariConfig(): HikariConfig = HikariConfig().apply {
        jdbcUrl = dbJdbcUrl
        driverClassName = "org.postgresql.Driver"
        minimumIdle = 1
        maxLifetime = 1800000
        maximumPoolSize = 10
        connectionTimeout = 100000
        idleTimeout = 100000
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    }

    override fun retrieveAuditLogSyncStatusesAsMap(): MutableMap<LocalDate, List<AuditLogSyncStatus>> {
        val dataSource = HikariDataSource(hikariConfig())
        val database = Database.connect(dataSource)
        val result = transaction(database) {
            AuditLogSyncStatusTable.selectAll()
                .map { it.toAuditLogSyncStatus() }
                .groupBy { it.syncDate }
                .toMutableMap()
        }
        dataSource.close()
        return result
    }

    override fun upsertAuditLogSyncStatus(eventDate: LocalDate, syncDate: LocalDate, entity: String, numberOfRecords: Int): AuditLogSyncStatus? {
        try {
            log.info { "Upserting audit log sync status for eventDate: $eventDate, entity: $entity, numberOfRecords: $numberOfRecords" }
            val dataSource = HikariDataSource(hikariConfig())
            val database = Database.connect(dataSource)
            val result = transaction(database) {
                AuditLogSyncStatusTable.upsert(
                    keys = arrayOf(AuditLogSyncStatusTable.eventDate, AuditLogSyncStatusTable.entity)
                ) {
                    it[AuditLogSyncStatusTable.eventDate] = eventDate
                    it[AuditLogSyncStatusTable.syncDate] = syncDate
                    it[AuditLogSyncStatusTable.entity] = entity
                    it[AuditLogSyncStatusTable.numberOfRecords] = numberOfRecords
                }
            }.resultedValues?.firstOrNull()?.toAuditLogSyncStatus()
            log.info { "Finnish upserting  $entity with , $numberOfRecords records" }
            dataSource.close()
            return result
        } catch (e: Exception) {
            log.error(e) { "Error while upserting audit log sync status for eventDate: $eventDate, entity: $entity" }
            return null
        }
    }

    override fun fetchAuditLogSyncStatus(eventDate: LocalDate): List<AuditLogSyncStatus> {
        val dataSource = HikariDataSource(hikariConfig())
        val database = Database.connect(dataSource)
        val result = transaction(database) {
            AuditLogSyncStatusTable.selectAll()
                .where {
                    (AuditLogSyncStatusTable.eventDate eq eventDate)
                }
                .map { it.toAuditLogSyncStatus() }
        }
        dataSource.close()
        return result
    }

    override fun fetchAuditLogSyncStatusByEntity(
        eventDate: LocalDate,
        entity: String
    ): List<AuditLogSyncStatus> {
        val dataSource = HikariDataSource(hikariConfig())
        val database = Database.connect(dataSource)
        val result = transaction(database) {
            AuditLogSyncStatusTable.selectAll()
                .where {
                    (AuditLogSyncStatusTable.eventDate eq eventDate) and (AuditLogSyncStatusTable.entity eq entity)
                }
                .map { it.toAuditLogSyncStatus() }
        }
        dataSource.close()
        return result
    }

    fun createStatusTable(dropFirst: Boolean = false) {
        val dataSource = HikariDataSource(hikariConfig())
        val database = Database.connect(dataSource)
        transaction(database) {
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
        dataSource.close()
    }
}
