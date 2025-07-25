package no.nav.sf.audit.logging.services

import no.nav.sf.audit.logging.model.UriEvent

class UriEventEntityFilter() {
    fun filterUriEventsWithEntitiesToBeLogged(entitiesToBeLogged: Map<String, String>, uriEvents: List<UriEvent>): List<UriEvent> {
        return uriEvents.filter { entitiesToBeLogged.containsKey(it.entity) && it.userType == "Standard" }
    }
}
