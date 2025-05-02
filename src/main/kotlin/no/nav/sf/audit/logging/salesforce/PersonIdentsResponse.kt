package no.nav.sf.audit.logging.salesforce

data class PersonIdentsResponse(val objectName: String, val numberOfRequests: Int, val personIdentByRecordId: Map<String, String>)
