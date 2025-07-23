package no.nav.sf.audit.logging.filters

import java.util.Properties
import kotlin.toString

class EntitiesFiltering {
    fun fetchEntitiesInObjectsYaml(): Map<String, String> {
        return try {
            EntitiesFiltering::class.java.getResourceAsStream("/objects.yaml")?.use { inputStream ->
                Properties().apply { load(inputStream) }
                    .entries.associate { it.key.toString() to it.value.toString() }
            } ?: throw IllegalStateException("Cannot find objects.yaml in resources")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load objects.yaml", e)
        }
    }

    fun fetchEntitiesToBeLogged(chosenEntity: String, entites: Map<String, String>): Map<String, String> {
        return if (chosenEntity == "All") {
            // "All" should not include Case. Cases are running in a separate job
            entites.filterKeys { it != "Case" }
        } else {
            val filteredEntities = entites.filterKeys { it == chosenEntity }
            if (filteredEntities.isEmpty()) {
                throw IllegalStateException("No objects found for entity: $chosenEntity")
            }
            filteredEntities
        }
    }
}
