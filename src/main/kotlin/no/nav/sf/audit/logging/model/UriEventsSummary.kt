package no.nav.sf.audit.logging.model

data class UriEventsSummary(val uriEventsWithPersonIdent: Double, val uriEventsWithoutAnyPersonIdents: Double) {
    val uriEventsWithPersonIdentInt: Int
        get() = uriEventsWithPersonIdent.toInt()
}
