package com.kinsync.android.network

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HealthApiClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: HealthApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = HealthApiClient(baseUrl = server.url("/").toString().removeSuffix("/"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `checkHealth returns Success on 200 response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok"}"""))

        val result = client.checkHealth()

        assertTrue(result is HealthCheckResult.Success)
        assertEquals("""{"status":"ok"}""", (result as HealthCheckResult.Success).rawBody)
    }

    @Test
    fun `checkHealth returns Failure on 503 response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = client.checkHealth()

        assertTrue(result is HealthCheckResult.Failure)
        assertEquals("HTTP 503", (result as HealthCheckResult.Failure).reason)
    }

    @Test
    fun `checkHealth returns Failure when server unreachable`() = runTest {
        server.shutdown()

        val result = client.checkHealth()

        assertTrue(result is HealthCheckResult.Failure)
    }

    @Test
    fun `checkHealth returns Failure reason on empty error response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody(""))

        val result = client.checkHealth()

        assertTrue(result is HealthCheckResult.Failure)
        assertEquals("HTTP 500", (result as HealthCheckResult.Failure).reason)
    }
}

