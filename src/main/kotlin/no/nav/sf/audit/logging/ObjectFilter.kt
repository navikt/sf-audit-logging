package no.nav.sf.audit.logging

import mu.KotlinLogging
import no.nav.sf.audit.logging.salesforce.UriEvent
import java.util.Properties

class ObjectFilter {
    val objectsToBeLogged get() = fetchObjectsToBeLogged()

    private val log = KotlinLogging.logger { }

    fun filterUriEventsToHaveObjectsToBeLogged(uriEvents: List<UriEvent>): List<UriEvent> {
        return uriEvents.filter { objectsToBeLogged.containsKey(it.entity) }
    }

    private fun fetchObjectsToBeLogged(): Properties {
        return try {
            ObjectFilter::class.java.getResourceAsStream("/objects.yaml")?.use { inputStream ->
                Properties().apply { load(inputStream) }
            } ?: throw IllegalStateException("Cannot find objects.yaml in resources")
        } catch (e: Exception) {
            log.error(e) { "Failed to load objects.yaml" }
            Properties()
        }
    }
}
