package no.nav.sf.audit.logging.salesforce

data class UriEvent(val eventDateString: String?, val entity: String?, val recordId: String?, val operation: String?, val username: String?, val userType: String?)
