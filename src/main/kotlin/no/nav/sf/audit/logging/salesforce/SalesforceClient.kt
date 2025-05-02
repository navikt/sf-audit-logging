package no.nav.sf.audit.logging.salesforce

interface SalesforceClient {
    fun fetchUriEvents(): MutableList<UriEvent>
    fun fetchPersonIdents(objectName: String, personIdentSelectClause: String, recordIds: List<String>): PersonIdentsResponse
}
