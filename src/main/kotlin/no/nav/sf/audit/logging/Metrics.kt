package no.nav.sf.audit.logging

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import mu.KotlinLogging
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import java.io.StringWriter

object Metrics {
    private val log = KotlinLogging.logger { }

    val uriEventsWithPersonIdent = registerLabelCounter("uri_events", "object")
    val uriEventsWithoutAnyPersonIdents = registerLabelCounter("uri_events_without_person_ident", "object")
    val numberOfApiCalls = registerLabelCounter("api_calls", "Salesforce")

    fun registerLabelCounter(name: String, vararg labels: String) =
        Counter.build().name(name).help(name).labelNames(*labels).register()

    fun clearUriEventsCounter() {
        uriEventsWithPersonIdent.clear()
        uriEventsWithoutAnyPersonIdents.clear()
        numberOfApiCalls.clear()
    }

    init {
        DefaultExports.initialize()
    }

    val metricsHttpHandler: HttpHandler = {
        try {
            val str = StringWriter()
            TextFormat.write004(str, CollectorRegistry.defaultRegistry.metricFamilySamples())
            val result = str.toString()
            if (result.isEmpty()) {
                Response(Status.NO_CONTENT)
            } else {
                Response(Status.OK).body(result)
            }
        } catch (e: Exception) {
            log.error { "/prometheus failed writing metrics - ${e.message}" }
            Response(Status.INTERNAL_SERVER_ERROR)
        }
    }
}
