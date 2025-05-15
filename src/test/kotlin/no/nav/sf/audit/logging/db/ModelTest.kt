package no.nav.sf.audit.logging.db

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModelTest {

    @Test
    fun `Should get metadata in JSON format`() {
        val metadata = getMetaData()
        assertTrue(metadata.contains("\"true\": 10"))
    }
}
