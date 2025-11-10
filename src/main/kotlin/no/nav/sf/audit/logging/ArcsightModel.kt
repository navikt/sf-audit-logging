package no.nav.sf.audit.logging

import no.nav.sf.audit.logging.model.UriEvent
import java.time.Instant

fun createLogMessage(record: UriEvent): String {
    val version = "CEF:0"
    val deviceVendor = "salesforce"
    val deviceProduct = "audit_logs"
    val deviceVersion = "1.0"
    val signatureID = "audit:accessed"
    val name = "AuditLogs"
    val severity = "INFO"
    val extension = createExtension(record)

    return listOf(
        version,
        deviceVendor,
        deviceProduct,
        deviceVersion,
        signatureID,
        name,
        severity,
        extension,
    ).joinToString(separator = "|")
}

private fun getUNIXTimestamp(date: String?): String = date?.let { Instant.parse(it.substring(0, 22) + "Z").toEpochMilli().toString() } ?: ""

private fun createExtension(uriEvent: UriEvent): String {
    val end = "end=" + getUNIXTimestamp(uriEvent.eventDate)
    val suid = "suid=" + uriEvent.username
    val flexString1 = "flexString1=" + uriEvent.entity
    val flexString1Label = "flexString1Label=" + uriEvent.entity
    val act = "act=" + uriEvent.operation
    val duid = "duid=" + uriEvent.personIdent

    return listOf(
        end,
        suid,
        flexString1,
        flexString1Label,
        act,
        duid,
    ).joinToString(separator = " ")
}
