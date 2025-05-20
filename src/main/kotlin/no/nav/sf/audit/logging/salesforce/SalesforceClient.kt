package no.nav.sf.audit.logging.salesforce

import java.time.LocalDate

interface SalesforceClient {
    fun fetchUriEvents(eventDate: LocalDate): MutableList<UriEvent>
    fun fetchPersonIdents(objectName: String, personIdentSelectClause: String, recordIds: List<String>): PersonIdentsResponse
}
