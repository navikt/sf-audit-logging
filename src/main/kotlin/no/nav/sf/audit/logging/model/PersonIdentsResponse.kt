package no.nav.sf.audit.logging.model

data class PersonIdentsResponse(val objectName: String, val numberOfApiCalls: Int, val personIdentByRecordId: Map<String, String>)
