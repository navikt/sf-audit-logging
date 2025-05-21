package no.nav.sf.audit.logging

import mu.KotlinLogging
import no.nav.sf.audit.logging.db.DefaultPostgresDatabase
import no.nav.sf.audit.logging.db.MockPostgresDatabase
import no.nav.sf.audit.logging.db.PostgresDatabase
import no.nav.sf.audit.logging.db.getMetaData
import no.nav.sf.audit.logging.salesforce.DefaultSalesforceClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.server.ApacheServer
import org.http4k.server.Http4kServer
import org.http4k.server.asServer

object Application {
    private val log = KotlinLogging.logger { }
    private val cluster = System.getenv(env_NAIS_CLUSTER_NAME) ?: "local"
    private val postgresDatabase: PostgresDatabase = if (local) MockPostgresDatabase() else DefaultPostgresDatabase()
    private val auditLogHandler = AuditLogHandler(DefaultSalesforceClient(), postgresDatabase)

    val context = env(config_CONTEXT)

    val gson = configureGson()

    fun apiServer(port: Int): Http4kServer = api().asServer(ApacheServer(port))

    fun api(): HttpHandler = routes(
        "/internal/isAlive" bind Method.GET to { Response(OK) },
        "/internal/isReady" bind Method.GET to { Response(OK) },
        "/internal/metrics" bind Method.GET to Metrics.metricsHttpHandler,
        "/internal/gui" bind Method.GET to static(ResourceLoader.Classpath("gui")),
        "/internal/guiLabel" bind Method.GET to { Response(OK).body(context) },
        "/internal/metadata" bind Method.GET to metaDataHandler,
        "/internal/fetchAndTransfer" bind Method.GET to auditLogHandler.fetchAndTransfer
    )

    fun start() {
        log.info { "Starting in cluster $cluster" }
        apiServer(8080).start()
    }

    private val metaDataHandler: HttpHandler = {
        Response(OK).body(getMetaData(postgresDatabase))
    }
}
