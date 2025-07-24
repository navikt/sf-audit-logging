package no.nav.sf.audit.logging.salesforce

import no.nav.sf.audit.logging.model.PersonIdentsResponse
import no.nav.sf.audit.logging.model.UriEvent
import java.time.LocalDate

interface SalesforceClient {
    fun fetchUriEvents(eventDate: LocalDate): List<UriEvent>
    fun fetchPersonIdents(objectName: String, personIdentSelectClause: String, recordIds: List<String>): PersonIdentsResponse
}
