package no.nav.sf.audit.logging.db

import java.time.LocalDate

object PostgresDatabase {

    val auditLogSyncStatusMap: MutableMap<LocalDate, MutableMap<Boolean, Int>> get() {
        return mutableMapOf()
    }
}
