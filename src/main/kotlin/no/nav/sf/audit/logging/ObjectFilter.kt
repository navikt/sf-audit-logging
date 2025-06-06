package no.nav.sf.audit.logging

import mu.KotlinLogging
import no.nav.sf.audit.logging.salesforce.UriEvent
import java.util.Properties

class ObjectFilter(val entity: String) {
    val objectsToBeLogged get() = fetchObjectsToBeLogged()

    private val log = KotlinLogging.logger { }

    fun filterUriEventsToHaveObjectsToBeLogged(uriEvents: List<UriEvent>): List<UriEvent> {
        log.info("Filtering URI events ${uriEvents.size} for entity: $entity")
        return uriEvents.filter { objectsToBeLogged.containsKey(it.entity) }
    }

    private fun fetchObjectsToBeLogged(): Properties {
        log.info("Fetching objects to be logged for entity: $entity")
        val allObjects = try {
            ObjectFilter::class.java.getResourceAsStream("/objects.yaml")?.use { inputStream ->
                Properties().apply { load(inputStream) }
            } ?: throw IllegalStateException("Cannot find objects.yaml in resources")
        } catch (e: Exception) {
            log.error(e) { "Failed to load objects.yaml" }
            Properties()
        }
        log.info("Loaded objects: ${allObjects.size}")
        return if (entity == "All") {
            allObjects
        } else {
            val filteredObjects = Properties()
            allObjects.forEach { key, value ->
                if (key.toString() == entity) {
                    filteredObjects[key] = value
                }
            }
            if (filteredObjects.isEmpty()) {
                log.warn { "No objects found for entity: $entity" }
            }
            log.info("Loaded objects - filtered: ${filteredObjects.size}")
            filteredObjects
        }
    }
}
