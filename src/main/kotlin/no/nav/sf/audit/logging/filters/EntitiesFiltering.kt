package no.nav.sf.audit.logging.filters

class EntitiesFiltering {
    fun getEntitiesToBeLoggedFromYaml(): Map<String, String> {
        return mapOf("key1" to "value1", "key2" to "value2")
    }
}
