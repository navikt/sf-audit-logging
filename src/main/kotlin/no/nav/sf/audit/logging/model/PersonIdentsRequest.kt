package no.nav.sf.audit.logging.model

data class PersonIdentsRequest(
    val objectName: String,
    val personIdentField: String,
    val recordIds: List<String>,
)
