package com.kinsync.android.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackendConfigTest {

    @Test
    fun `requireHttps returns the same URL when already https`() {
        val result = BackendConfig.requireHttps("https://kinsync.example.com")

        assertEquals("https://kinsync.example.com", result)
    }

    @Test
    fun `requireHttps rejects http URLs`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackendConfig.requireHttps("http://kinsync.example.com")
        }
    }

    @Test
    fun `requireHttps rejects malformed URLs`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackendConfig.requireHttps("not-a-url")
        }
    }

    @Test
    fun `requireHttps rejects empty string`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackendConfig.requireHttps("")
        }
    }
}
