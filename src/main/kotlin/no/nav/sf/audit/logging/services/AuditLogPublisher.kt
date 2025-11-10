package no.nav.sf.audit.logging.services

import no.nav.sf.audit.logging.model.PersonIdentsResponse
import no.nav.sf.audit.logging.model.UriEvent
import no.nav.sf.audit.logging.model.UriEventsSummary

interface AuditLogPublisher {
    fun publishLogs(
        uriEvents: List<UriEvent>,
        personIdentsResponse: PersonIdentsResponse,
        offset: Int,
    ): UriEventsSummary
}
