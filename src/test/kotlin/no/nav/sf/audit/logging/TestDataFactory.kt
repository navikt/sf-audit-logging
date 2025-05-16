package no.nav.sf.audit.logging

import no.nav.sf.audit.logging.salesforce.UriEvent

object TestDataFactory {

    fun getUriEvents(numberOfRecords: Int): MutableList<UriEvent> {
        val uriEvents = mutableListOf<UriEvent>()

        for (i in 1..numberOfRecords) {
            uriEvents.add(
                UriEvent(
                    eventDate = "2023-06-14T10:30:00.000+0000",
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
