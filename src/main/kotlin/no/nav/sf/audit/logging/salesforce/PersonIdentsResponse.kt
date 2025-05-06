package no.nav.sf.audit.logging.salesforce

data class PersonIdentsResponse(val objectName: String, val numberOfApiCalls: Int, val personIdentByRecordId: Map<String, String>)
