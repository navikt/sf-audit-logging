package no.nav.sf.audit.logging.db

import ch.qos.logback.core.model.Model
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue


class ModelTest {

    @Test
    fun `Should get metadata in JSON format`() {
        val metadata = getMetaData()
        assertEquals("test", metadata)
        assertTrue(metadata.contains("\"true\": 10"))
    }
}