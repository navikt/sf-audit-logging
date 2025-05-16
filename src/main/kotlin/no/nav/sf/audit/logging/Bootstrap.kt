package no.nav.sf.audit.logging

val local = env(env_NAIS_CLUSTER_NAME) == "local"

fun main() = Application.start()
