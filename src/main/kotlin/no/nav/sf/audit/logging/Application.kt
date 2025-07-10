package no.nav.sf.audit.logging

import mu.KotlinLogging
import no.nav.sf.audit.logging.db.PostgresDatabase
import no.nav.sf.audit.logging.db.getMetaData
import no.nav.sf.audit.logging.plugins.appModule
import no.nav.sf.audit.logging.salesforce.SalesforceClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext.startKoin
import java.time.LocalDate

object Application : KoinComponent {
    private val log = KotlinLogging.logger { }
    private val cluster = System.getenv(env_NAIS_CLUSTER_NAME) ?: "local"
    private val salesforceClient by inject<SalesforceClient>()
    private val postgresDatabase by inject<PostgresDatabase>()

    val context = env(config_CONTEXT)
    val gson = configureGson()

    fun apiServer(port: Int): Http4kServer = api().asServer(Netty(port))

    fun api(): HttpHandler = routes(
        "/internal/isAlive" bind Method.GET to { Response(OK) },
        "/internal/isReady" bind Method.GET to { Response(OK) },
        "/internal/metrics" bind Method.GET to Metrics.metricsHttpHandler,
        "/internal/gui" bind Method.GET to static(ResourceLoader.Classpath("gui")),
        "/internal/guiLabel" bind Method.GET to { Response(OK).body(context) },
        "/internal/metadata" bind Method.GET to metaDataHandler,
        "/internal/fetchAndLog" bind Method.GET to auditLogHandler,
        "/internal/clearDb" bind Method.GET to clearDbHandler,
        "/internal/initDb" bind Method.GET to initDbHandler
    )

    fun start() {
        startKoin {
            modules(appModule)
        }

        log.info { "Starting in cluster $cluster" }
        apiServer(8080).start()
    }

    private val auditLogHandler: HttpHandler = {
        val eventDateParam = it.query("eventDate")
        val eventDate = eventDateParam?.let { date: String -> LocalDate.parse(date) } ?: LocalDate.now().minusDays(1)
        val entityParam = it.query("entity")
        val entity = entityParam?.let { entity: String -> entity } ?: "All"
        val offsetParam = it.query("offset")
        val offset = offsetParam?.let { offset: String -> offset.toInt() } ?: 0

        if (AuditLogJob.active) {
            log.info("Audit log job is already active, cannot start a new one")
            Response(OK).body("Audit log job is already active, cannot start a new one")
        } else {
            AuditLogJob.activateFetchAndLog(eventDate, entity, offset, salesforceClient, postgresDatabase)
            Response(OK).body("Start logging for event date $eventDate and entity $entity")
        }
    }

    private val metaDataHandler: HttpHandler = {
        Response(OK).body(getMetaData())
    }

    private val clearDbHandler: HttpHandler = {
        postgresDatabase.createStatusTable(true)
        Response(OK).body("Table recreated")
    }

    private val initDbHandler: HttpHandler = {
        postgresDatabase.createStatusTable(false)
        Response(OK).body("Table created")
    }
}
