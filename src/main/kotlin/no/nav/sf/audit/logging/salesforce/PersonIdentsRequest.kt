package no.nav.sf.audit.logging.salesforce

data class PersonIdentsRequest(val objectName: String, val personIdentField: String, val recordIds: List<String>)
