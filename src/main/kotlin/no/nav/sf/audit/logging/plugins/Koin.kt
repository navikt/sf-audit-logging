package no.nav.sf.audit.logging.plugins

import no.nav.sf.audit.logging.db.DefaultPostgresDatabase
import no.nav.sf.audit.logging.db.MockPostgresDatabase
import no.nav.sf.audit.logging.db.PostgresDatabase
import no.nav.sf.audit.logging.local
import no.nav.sf.audit.logging.salesforce.DefaultSalesforceClient
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import no.nav.sf.audit.logging.services.AuditLogPublisher
import no.nav.sf.audit.logging.services.AuditLogSyncJobMonitor
import no.nav.sf.audit.logging.services.DefaultAuditLogPublisher
import no.nav.sf.audit.logging.services.PostgresAuditLogSyncJobMonitor
import no.nav.sf.audit.logging.token.DefaultAccessTokenHandler
import org.http4k.client.OkHttp
import org.koin.dsl.module

val appModule = module {
    single<SalesforceClient> { DefaultSalesforceClient(DefaultAccessTokenHandler(), OkHttp()) }
    single<PostgresDatabase> { if (local) MockPostgresDatabase() else DefaultPostgresDatabase() }
    single<AuditLogSyncJobMonitor> { PostgresAuditLogSyncJobMonitor(DefaultPostgresDatabase()) }
    single<AuditLogPublisher> { DefaultAuditLogPublisher() }
}
