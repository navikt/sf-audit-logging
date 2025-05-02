package no.nav.sf.audit.logging.salesforce

import io.mockk.every
import io.mockk.mockk
import no.nav.sf.audit.logging.token.AccessTokenHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Response.Companion.invoke
import org.http4k.core.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DefaultSalesforceClientTest {

    private val accessTokenHandler: AccessTokenHandler = mockk<AccessTokenHandler> {
        every { accessToken } returns "mockAccessToken"
        every { instanceUrl } returns "https://mock-instance-url.com"
        every { tenantId } returns "mockTenantId"
    }
    private val client = mockk<(Request) -> Response>()
    private val classUnderTest = DefaultSalesforceClient(accessTokenHandler, client)

    @Test
    fun `should return one uri event when Salesforce query returns one event`() {
        val mockResponseBody = "{\"totalSize\":1,\"done\":true,\"records\":[{\"EventDate\": \"2025-04-24T13:19:30.102+0000\",\"Operation\": \"Read\",\"QueriedEntities\": \"Account\",\"RecordId\": \"0015t00000xYQl6AAG\",\"Username\": \"user@nav.no.sit2\",\"UserType\": \"Standard\"}]}"
        val mockResponse = Response(Status.OK).body(mockResponseBody)
        every { client(any()) } returns mockResponse

        val result = classUnderTest.fetchUriEvents()
        assertEquals(1, result.size)
    }

    @Test
    fun `should return event date 24th April 2025 when event date is 2025-04-24`() {
        val mockResponseBody = "{\"totalSize\":1,\"done\":true,\"records\":[{\"EventDate\": \"2025-04-24T13:19:30.102+0000\",\"Operation\": \"Read\",\"QueriedEntities\": \"Account\",\"RecordId\": \"0015t00000xYQl6AAG\",\"Username\": \"user@nav.no.sit2\",\"UserType\": \"Standard\"}]}"
        val mockResponse = Response(Status.OK).body(mockResponseBody)
        every { client(any()) } returns mockResponse

        val expectedDateTime = ZonedDateTime.parse("2025-04-24T13:19:30.102+0000", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
        val result = classUnderTest.fetchUriEvents()
        assertEquals(expectedDateTime, result[0].eventDate)
    }

    @Test
    fun `should return a map with one person ident when one of the two accounts has a person ident in Salesforce`() {
        // only the first account has a person ident
        val mockResponseBody = "{\"totalSize\":2,\"done\":true,\"records\":[{\"attributes\":{\"type\":\"Account\",\"url\":\"/services/data/v62.0/sobjects/Account/0015t00000HvTteAAF\"},\"Id\":\"0015t00000HvTteAAF\",\"INT_PersonIdent__c\":\"14097018384\"},{\"attributes\":{\"type\":\"Account\",\"url\":\"/services/data/v62.0/sobjects/Account/0015t00000I34yqAAB\"},\"Id\":\"0015t00000I34yqAAB\",\"INT_PersonIdent__c\":null}]}"
        val mockResponse = Response(Status.OK).body(mockResponseBody)
        every { client(any()) } returns mockResponse

        val recordIds = listOf("0015t00000HvTteAAF", "0015t00000I34yqAAB")
        val result = classUnderTest.fetchPersonIdents("Account", "INT_PersonIdent__c", recordIds)

        assertEquals(1, result.personIdentByRecordId.size)
        assertEquals("14097018384", result.personIdentByRecordId["0015t00000HvTteAAF"])
        assertEquals(1, result.numberOfRequests)
    }

    @Test
    fun `should perform 12 requests to Salesforce when there are 22100 record IDs`() {
        val recordIds = mutableListOf<String>()
        for (i in 1..22100) {
            recordIds.add("0015t00000HvTteAAF$i")
        }
        val mockResponseBody = "{\"totalSize\":2,\"done\":true,\"records\":[{\"attributes\":{\"type\":\"Account\",\"url\":\"/services/data/v62.0/sobjects/Account/0015t00000HvTteAAF\"},\"Id\":\"0015t00000HvTteAAF\",\"INT_PersonIdent__c\":\"14097018384\"}]}"

        val mockResponse = Response(Status.OK).body(mockResponseBody)
        every { client(any()) } returns mockResponse

        val result = classUnderTest.fetchPersonIdents("Account", "INT_PersonIdent__c", recordIds)

        assertEquals(12, result.numberOfRequests)
    }
}
