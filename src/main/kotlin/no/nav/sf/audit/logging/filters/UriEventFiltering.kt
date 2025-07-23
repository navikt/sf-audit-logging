package no.nav.sf.audit.logging.filters

import no.nav.sf.audit.logging.salesforce.UriEvent

class UriEventFiltering(private val entitiesToBeLogged: Map<String, String>) {
    fun filterUriEventsWithEntitiesToBeLogged(uriEvents: List<UriEvent>): List<UriEvent> {
        return uriEvents.filter { entitiesToBeLogged.containsKey(it.entity) && it.userType == "Standard" }
    }
}
