package no.nav.sf.audit.logging.filters

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EntitiesFilteringTest {
    private val classUnderTest = EntitiesFiltering()
    private val entities = mutableMapOf<String, String>()

    init {
        entities["Account"] = "INT_PersonIdent__c"
        entities["NavTask__c"] = "INT_PersonIdent__c"
        entities["Case"] = "INT_CaseIdent__c"
    }

    @Test
    fun `Should refer to the person ident field on Accounts`() {
        val result = classUnderTest.fetchEntitiesInObjectsYaml()["Account"]
        assertEquals("INT_PersonIdent__c", result)
    }

    @Test
    fun `Should be more than one objects to be logged when entity is set to All`() {
        val objectsToBeLogged = classUnderTest.fetchEntitiesToBeLogged("All", entities)
        assertTrue(objectsToBeLogged.size > 1)
    }

    @Test
    fun `Should be one object to be logged when entity is set to Case`() {
        val objectsToBeLogged = classUnderTest.fetchEntitiesToBeLogged("Case", entities)
        assertEquals(1, objectsToBeLogged.size)
    }
}
