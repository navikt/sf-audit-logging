package no.nav.sf.audit.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObjectFilterTest {
    val classUnderTest = ObjectFilter()

    @Test
    fun `Should refer to the Person Name field for Accounts`() {
        val result = classUnderTest.objectsToBeLogged.getProperty("Account")
        assertEquals(result, "Person__r.Name")
    }

    @Test
    fun `Should be more than one object to be logged`() {
        val result = classUnderTest.objectsToBeLogged
        assertTrue(result.size > 1)
    }
}
