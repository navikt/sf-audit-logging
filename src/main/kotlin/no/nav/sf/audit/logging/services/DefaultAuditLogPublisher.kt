package no.nav.sf.audit.logging.services

import mu.KotlinLogging
import no.nav.sf.audit.logging.createLogMessage
import no.nav.sf.audit.logging.model.PersonIdentsResponse
import no.nav.sf.audit.logging.model.UriEvent
import no.nav.sf.audit.logging.model.UriEventsSummary
import kotlin.collections.forEach

class DefaultAuditLogPublisher : AuditLogPublisher {
    private val naudit = KotlinLogging.logger("AuditLogger")
    override fun publishLogs(uriEvents: List<UriEvent>, personIdentsResponse: PersonIdentsResponse, offset: Int): UriEventsSummary {
        var uriEventsWithPersonIdent = 0.0
        var uriEventsWithoutAnyPersonIdents = 0.0
        var batchCounter = 0
        var indexForOffset = 0
        uriEvents.forEach { event ->
            val personIdent = personIdentsResponse.personIdentByRecordId[event.recordId]
            if (personIdent != null) {
                indexForOffset++
                if (indexForOffset - 1 < offset) {
                    return@forEach // Skip this event if it is before the offset
                }
                event.personIdent = personIdent
                uriEventsWithPersonIdent += 1.0
                naudit.info(createLogMessage(event))
                batchCounter++
                if (batchCounter == 100) {
                    Thread.sleep(2000) // Pause for 2 seconds
                    batchCounter = 0
                }
            } else {
                uriEventsWithoutAnyPersonIdents += 1.0
            }
        }
        return UriEventsSummary(uriEventsWithPersonIdent, uriEventsWithoutAnyPersonIdents)
    }
}
