
import no.nav.sf.audit.logging.createLogMessage
import no.nav.sf.audit.logging.salesforce.UriEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArcsightModelTest {
    @Test
    fun `Should transform uri event to Arcsight - eventdate will be UTC millis`() {
        val uriEvent = UriEvent(
            eventDate = "2023-06-14T10:30:00.000+0000",
            entity = "Account",
            recordId = "a98893bc7",
            operation = "CREATE",
            username = "john.doe"
        )
        uriEvent.personIdent = "123456789"

        val expectedLogMessage = "CEF:0|salesforce|audit_logs|1.0|audit:accessed|AuditLogs|INFO|end=1686738600000 suid=john.doe flexString1=Account flexString1Label=Account act=CREATE duid=123456789"
        val actualLogMessage = createLogMessage(uriEvent)

        assertEquals(expectedLogMessage, actualLogMessage)
    }
}
