package no.nav.sf.audit.logging.db

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModelTest {

    @Test
    fun `Should get metadata in JSON format`() {
        val metadata = getMetaData(MockPostgresDatabase())

        assertTrue(metadata.contains("\"numberOfRecords\": 45") && metadata.contains("\"success\": true"))
        assertTrue(metadata.contains("\"numberOfRecords\": 5") && metadata.contains("\"success\": false"))
    }
}
