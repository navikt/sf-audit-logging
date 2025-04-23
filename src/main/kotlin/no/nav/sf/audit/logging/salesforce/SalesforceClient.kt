package no.nav.sf.audit.logging.salesforce

interface SalesforceClient {
    fun fetchUriEvents(): MutableList<UriEvent>
}
