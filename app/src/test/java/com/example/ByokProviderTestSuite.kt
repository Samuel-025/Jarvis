package com.example

import com.example.core.ai.provider.AIProvider
import com.example.core.ai.provider.AIRequest
import com.example.core.ai.provider.AIResponseResult
import com.example.core.ai.provider.ConnectionState
import com.example.core.ai.provider.LocalOfflineProvider
import com.example.core.ai.provider.OpenAIProviderAdapter
import com.example.core.ai.provider.ProviderCapability
import com.example.core.ai.provider.ProviderConfiguration
import com.example.core.ai.provider.ProviderErrorType
import com.example.core.ai.provider.ProviderType
import com.example.core.ai.storage.ApiKeyStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MemoryApiKeyStorage : ApiKeyStorage {
    private val map = mutableMapOf<String, String>()

    override fun saveKey(providerKeyAlias: String, rawApiKey: String): Boolean {
        if (rawApiKey.isBlank()) {
            map.remove(providerKeyAlias)
        } else {
            map[providerKeyAlias] = rawApiKey
        }
        return true
    }

    override fun getKey(providerKeyAlias: String): String? = map[providerKeyAlias]

    override fun removeKey(providerKeyAlias: String): Boolean {
        map.remove(providerKeyAlias)
        return true
    }

    override fun hasKey(providerKeyAlias: String): Boolean = !map[providerKeyAlias].isNullOrBlank()

    override fun clearAll(): Boolean {
        map.clear()
        return true
    }
}

class ByokProviderTestSuite {

    private lateinit var storage: ApiKeyStorage

    @Before
    fun setup() {
        storage = MemoryApiKeyStorage()
    }

    @Test
    fun testSavingReplacingAndRemovingApiKey() {
        assertFalse(storage.hasKey("GEMINI"))
        assertNull(storage.getKey("GEMINI"))

        // Save key
        assertTrue(storage.saveKey("GEMINI", "test-gemini-key-12345"))
        assertTrue(storage.hasKey("GEMINI"))
        assertEquals("test-gemini-key-12345", storage.getKey("GEMINI"))

        // Replace key
        assertTrue(storage.saveKey("GEMINI", "new-replacement-key-67890"))
        assertEquals("new-replacement-key-67890", storage.getKey("GEMINI"))

        // Remove key
        assertTrue(storage.removeKey("GEMINI"))
        assertFalse(storage.hasKey("GEMINI"))
        assertNull(storage.getKey("GEMINI"))
    }

    @Test
    fun testLocalOfflineProviderNeverRequiresKey() = runBlocking {
        val local = LocalOfflineProvider()
        assertFalse(local.descriptor.requiresApiKey)
        assertTrue(local.supportsCapability(ProviderCapability.TEXT_GENERATION, "jarvis-local-heuristic"))
        assertFalse(local.supportsCapability(ProviderCapability.IMAGE_UNDERSTANDING, "jarvis-local-heuristic"))

        val status = local.testConnection(
            ProviderConfiguration(ProviderType.LOCAL_OFFLINE, "jarvis-local-heuristic"),
            null
        )
        assertEquals(ConnectionState.CONNECTED, status.state)

        val result = local.query(
            AIRequest("Status check"),
            ProviderConfiguration(ProviderType.LOCAL_OFFLINE, "jarvis-local-heuristic"),
            null
        )
        assertTrue(result is AIResponseResult.Success)
        val success = result as AIResponseResult.Success
        assertEquals(ProviderType.LOCAL_OFFLINE, success.providerType)
        assertTrue(success.text.contains("JARVIS Local Offline Response"))
    }

    @Test
    fun testOpenAIAdapterMissingKeyError() = runBlocking {
        val adapter = OpenAIProviderAdapter()
        assertTrue(adapter.descriptor.requiresApiKey)

        val result = adapter.query(
            AIRequest("Hello"),
            ProviderConfiguration(ProviderType.OPENAI, "gpt-4o-mini"),
            null // No key passed
        )
        assertTrue(result is AIResponseResult.Error)
        val err = result as AIResponseResult.Error
        assertEquals(ProviderErrorType.MISSING_KEY, err.errorType)
        assertEquals(ProviderType.OPENAI, err.providerType)
    }

    @Test
    fun testProviderCapabilitiesValidation() {
        val openAI = OpenAIProviderAdapter()
        assertTrue(openAI.supportsCapability(ProviderCapability.TEXT_GENERATION, "gpt-4o-mini"))
        assertTrue(openAI.supportsCapability(ProviderCapability.IMAGE_UNDERSTANDING, "gpt-4o-mini"))

        val local = LocalOfflineProvider()
        assertTrue(local.supportsCapability(ProviderCapability.TEXT_GENERATION, "jarvis-local-heuristic"))
        assertFalse(local.supportsCapability(ProviderCapability.IMAGE_UNDERSTANDING, "jarvis-local-heuristic"))
    }

    @Test
    fun testAIProviderManagerRegistrationAndDefaultFallback() = runBlocking {
        val manager = com.example.core.ai.provider.AIProviderManager(storage)
        assertNotNull(manager.getProvider(ProviderType.GEMINI))
        assertNotNull(manager.getProvider(ProviderType.OPENAI))
        assertNotNull(manager.getProvider(ProviderType.OPENAI_COMPATIBLE))
        assertNotNull(manager.getProvider(ProviderType.LOCAL_OFFLINE))

        // Set provider to LOCAL_OFFLINE: should query without any API key
        manager.setSelectedProvider(ProviderType.LOCAL_OFFLINE)
        val resp = manager.query("Hello offline JARVIS", com.example.core.model.PrivacyMode.CLOUD)
        assertTrue(resp is com.example.core.ai.AIResponse.Success)
        assertTrue((resp as com.example.core.ai.AIResponse.Success).text.contains("JARVIS Local Offline Response"))

        // STRICT mode must route locally even when a cloud provider is selected.
        manager.setSelectedProvider(ProviderType.OPENAI)
        val strictResp = manager.query("Hello", com.example.core.model.PrivacyMode.STRICT)
        assertTrue(strictResp is com.example.core.ai.AIResponse.Success)
        assertTrue((strictResp as com.example.core.ai.AIResponse.Success).text.contains("JARVIS Local Offline Response"))
    }
}
