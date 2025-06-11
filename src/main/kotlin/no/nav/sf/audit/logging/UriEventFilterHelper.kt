package no.nav.sf.audit.logging

import mu.KotlinLogging
import no.nav.sf.audit.logging.salesforce.UriEvent
import java.util.Properties

class UriEventFilterHelper(val entity: String) {
    private val log = KotlinLogging.logger { }
    val objectsToBeLogged = fetchObjectsToBeLogged()

    fun filterUriEventsToHaveObjectsToBeLogged(uriEvents: List<UriEvent>): List<UriEvent> {
        return uriEvents.filter { objectsToBeLogged.containsKey(it.entity) && it.userType == "Standard" }
    }

    private fun fetchObjectsToBeLogged(): Properties {
        val allObjects = try {
            UriEventFilterHelper::class.java.getResourceAsStream("/objects.yaml")?.use { inputStream ->
                Properties().apply { load(inputStream) }
            } ?: throw IllegalStateException("Cannot find objects.yaml in resources")
        } catch (e: Exception) {
            log.error(e) { "Failed to load objects.yaml" }
            Properties()
        }
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
            filteredObjects
        }
    }
}
