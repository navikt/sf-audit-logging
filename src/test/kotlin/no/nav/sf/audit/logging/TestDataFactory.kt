package no.nav.sf.audit.logging

import no.nav.sf.audit.logging.salesforce.UriEvent

object TestDataFactory {

    fun getUriEvents(numberOfRecords: Int): MutableList<UriEvent> {
        val uriEvents = mutableListOf<UriEvent>()

        for (i in 1..numberOfRecords) {
            uriEvents.add(
                getUriEvent(i, "Account")
            )
        }
        return uriEvents
    }

    fun getUriEvent(index: Int, entity: String): UriEvent {
        return UriEvent(
            eventDate = "2023-06-14T10:30:00.000+0000",
            username = "testuser$index",
            entity = entity,
            operation = "CREATE",
            recordId = index.toString(),
            userType = "Standard"
        )
    }
}
