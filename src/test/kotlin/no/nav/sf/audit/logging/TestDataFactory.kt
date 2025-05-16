package no.nav.sf.audit.logging

import no.nav.sf.audit.logging.salesforce.UriEvent

object TestDataFactory {

    fun getUriEvents(numberOfRecords: Int): MutableList<UriEvent> {
        val uriEvents = mutableListOf<UriEvent>()

        for (i in 1..numberOfRecords) {
            uriEvents.add(
                UriEvent(
                    eventDate = "2023-10-01T12:00:00Z",
                    username = "testuser$i",
                    entity = "Account",
                    operation = "CREATE",
                    recordId = i.toString()
                )
            )
        }
        return uriEvents
    }
}
