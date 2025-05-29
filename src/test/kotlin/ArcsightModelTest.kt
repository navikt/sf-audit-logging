
import no.nav.sf.audit.logging.TestDataFactory
import no.nav.sf.audit.logging.createLogMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArcsightModelTest {
    @Test
    fun `Should transform uri event to Arcsight - eventdate will be UTC millis`() {
        val uriEvent = TestDataFactory.getUriEvents(1).first()
        uriEvent.personIdent = "123456789"

        val expectedLogMessage = "CEF:0|salesforce|audit_logs|1.0|audit:accessed|AuditLogs|INFO|end=1686738600000 suid=testuser1 flexString1=Account flexString1Label=Account act=CREATE duid=123456789"
        val actualLogMessage = createLogMessage(uriEvent)

        assertEquals(expectedLogMessage, actualLogMessage)
    }
}
