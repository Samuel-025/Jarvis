package com.example.core.di

import android.content.Context
import com.example.core.agent.BoundedAgentExecutor
import com.example.core.agent.BoundedAgentPlanner
import com.example.core.ai.AIBrain
import com.example.core.ai.GeminiProvider
import com.example.core.audit.AuditEventRepository
import com.example.core.engine.LocalCommandEngine
import com.example.core.engine.LocalCommandEngineImpl
import com.example.core.integrations.AndroidSystemIntegrationAdapter
import com.example.core.integrations.CalendarIntegrationAdapter
import com.example.core.integrations.ExternalWeatherAdapter
import com.example.core.integrations.IntegrationAdapter
import com.example.core.memory.JarvisDatabase
import com.example.core.memory.PersonalOsRepository
import com.example.core.phone.PhoneAutomationRegistry
import com.example.core.router.IntentRouter
import com.example.core.skills.SkillRegistry
import com.example.core.tools.ToolRegistry
import com.example.core.vision.TestStubVisionProvider
import com.example.core.vision.VisionProvider
import com.example.core.voice.AndroidSpeechRecognizerAdapter
import com.example.core.voice.AndroidTextToSpeechAdapter
import com.example.core.voice.SpeechRecognizerAdapter
import com.example.core.voice.TextToSpeechAdapter

class ServiceLocator(val appContext: Context) {

    val database: JarvisDatabase by lazy {
        JarvisDatabase.getDatabase(appContext)
    }

    val personalOsRepository: PersonalOsRepository by lazy {
        PersonalOsRepository(
            memoryDao = database.memoryDao(),
            noteDao = database.noteDao(),
            taskDao = database.taskDao(),
            routineDao = database.routineDao(),
            conversationDao = database.conversationDao()
        )
    }

    val auditEventRepository: AuditEventRepository by lazy {
        AuditEventRepository()
    }

    val localCommandEngine: LocalCommandEngine by lazy {
        LocalCommandEngineImpl(appContext)
    }

    val phoneAutomationRegistry: PhoneAutomationRegistry by lazy {
        PhoneAutomationRegistry(appContext)
    }

    val apiKeyStorage: com.example.core.ai.storage.ApiKeyStorage by lazy {
        com.example.core.ai.storage.EncryptedKeystoreApiKeyStorage(appContext)
    }

    val aiProviderManager: com.example.core.ai.provider.AIProviderManager by lazy {
        com.example.core.ai.provider.AIProviderManager(apiKeyStorage, appContext)
    }

    val aiBrain: AIBrain by lazy {
        aiProviderManager
    }

    val visionProvider: VisionProvider by lazy {
        com.example.core.vision.RealVisionProvider(aiBrain)
    }

    val speechRecognizerAdapter: SpeechRecognizerAdapter by lazy {
        AndroidSpeechRecognizerAdapter(appContext)
    }

    val textToSpeechAdapter: TextToSpeechAdapter by lazy {
        AndroidTextToSpeechAdapter(appContext)
    }

    val toolRegistry: ToolRegistry by lazy {
        ToolRegistry()
    }

    val skillRegistry: SkillRegistry by lazy {
        SkillRegistry()
    }

    val integrations: List<IntegrationAdapter> by lazy {
        listOf(
            AndroidSystemIntegrationAdapter(),
            CalendarIntegrationAdapter(),
            ExternalWeatherAdapter()
        )
    }

    val agentPlanner: BoundedAgentPlanner by lazy {
        BoundedAgentPlanner()
    }

    val intentRouter: IntentRouter by lazy {
        IntentRouter(
            localCommandEngine = localCommandEngine,
            phoneRegistry = phoneAutomationRegistry,
            personalOsRepository = personalOsRepository,
            aiBrain = aiBrain,
            toolRegistry = toolRegistry,
            auditEventRepository = auditEventRepository
        )
    }

    val agentExecutor: BoundedAgentExecutor by lazy {
        BoundedAgentExecutor(intentRouter)
    }

    companion object {
        @Volatile
        private var instance: ServiceLocator? = null

        fun getInstance(context: Context): ServiceLocator {
            return instance ?: synchronized(this) {
                instance ?: ServiceLocator(context.applicationContext).also { instance = it }
            }
        }
    }
}
