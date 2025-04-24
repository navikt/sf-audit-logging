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
}
