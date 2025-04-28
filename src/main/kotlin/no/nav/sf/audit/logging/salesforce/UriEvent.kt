package no.nav.sf.audit.logging.salesforce

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class UriEvent(val eventDateString: String, val entity: String, val recordId: String, val operation: String, val username: String, val userType: String) {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    val eventDate: ZonedDateTime?
        get() {
            return eventDateString.let {
                ZonedDateTime.parse(it, formatter)
            }
        }
}
