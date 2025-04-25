package no.nav.sf.audit.logging

import mu.KotlinLogging
import java.util.Properties

class ObjectFilter {
    private val log = KotlinLogging.logger { }

    fun getObjectsToBeLogged(): Properties {
        val properties = Properties()
        try {
            val inputStream = ObjectFilter::class.java.getResourceAsStream("/objects.yaml")
                ?: throw IllegalStateException("Cannot find objects.yaml in resources")
            inputStream.use {
                properties.load(it)
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to load objects.yaml" }
        }
        return properties
    }
}
