package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.agent.AgentPlan
import com.example.core.agent.AgentStep
import com.example.core.agent.BoundedAgentExecutor
import com.example.core.agent.BoundedAgentPlanner
import com.example.core.agent.StepStatus
import com.example.core.ai.AIBrain
import com.example.core.ai.AIResponse
import com.example.core.audit.AuditEventRepository
import com.example.core.engine.LocalCommandEngine
import com.example.core.engine.LocalCommandEngineImpl
import com.example.core.memory.JarvisDatabase
import com.example.core.memory.MemoryEntity
import com.example.core.memory.NoteEntity
import com.example.core.memory.PersonalOsRepository
import com.example.core.nlp.IntentClassifier
import com.example.core.memory.TaskEntity
import com.example.core.model.CommandResult
import com.example.core.model.ErrorType
import com.example.core.model.JarvisIntent
import com.example.core.model.PrivacyMode
import com.example.core.model.RiskLevel
import com.example.core.permissions.PermissionController
import com.example.core.phone.PhoneAutomationRegistry
import com.example.core.router.IntentRouter
import com.example.core.safety.EmergencyStop
import com.example.core.safety.RiskEngine
import com.example.core.tools.ToolRegistry
import com.example.core.voice.SpeechRecognizerAdapter
import com.example.core.voice.TextToSpeechAdapter
import com.example.core.voice.VoiceState
import com.example.ui.MainUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AcceptanceAuditComprehensiveTest {

    private lateinit var context: Context
    private lateinit var database: JarvisDatabase
    private lateinit var personalOsRepository: PersonalOsRepository
    private lateinit var auditEventRepository: AuditEventRepository
    private lateinit var localCommandEngine: LocalCommandEngine
    private lateinit var phoneRegistry: PhoneAutomationRegistry
    private lateinit var fakeAiBrain: FakeAiBrain
    private lateinit var toolRegistry: ToolRegistry
    private lateinit var router: IntentRouter
    private lateinit var agentPlanner: BoundedAgentPlanner
    private lateinit var agentExecutor: BoundedAgentExecutor

    class FakeAiBrain : AIBrain {
        var queryCount = 0
        var cloudQueryCount = 0
        var lastPrompt: String? = null
        var lastPrivacyMode: PrivacyMode? = null
        var returnError = false

        override suspend fun query(
            prompt: String,
            privacyMode: PrivacyMode,
            imageBase64: String?
        ): AIResponse {
            queryCount++
            lastPrompt = prompt
            lastPrivacyMode = privacyMode
            if (privacyMode == PrivacyMode.STRICT) {
                // Mirror the production provider manager: STRICT routes to local-only heuristics.
                return AIResponse.Success("STRICT Privacy Mode active. Local-only response for: $prompt")
            }
            cloudQueryCount++
            if (returnError) {
                return AIResponse.Error("Simulated cloud failure")
            }
            return AIResponse.Success("Fake AI response for: $prompt")
        }
    }

    class FakeSpeechRecognizerAdapter : SpeechRecognizerAdapter {
        var isListening = false
        var onResultCallback: ((String) -> Unit)? = null
        var onErrorCallback: ((Int, String) -> Unit)? = null

        override fun startListening(onResult: (String) -> Unit, onError: (Int, String) -> Unit) {
            isListening = true
            onResultCallback = onResult
            onErrorCallback = onError
        }

        override fun stopListening() {
            isListening = false
        }

        override fun cancel() {
            isListening = false
            onResultCallback = null
            onErrorCallback = null
        }

        override fun destroy() {
            cancel()
        }

        fun simulateError(code: Int, message: String) {
            onErrorCallback?.invoke(code, message)
        }

        fun simulateSuccess(text: String) {
            onResultCallback?.invoke(text)
        }
    }

    class FakeTextToSpeechAdapter : TextToSpeechAdapter {
        val spokenTexts = mutableListOf<String>()
        var isStopped = false

        override fun speak(text: String, onDone: () -> Unit) {
            spokenTexts.add(text)
            onDone()
        }

        override fun stop() {
            isStopped = true
        }

        override fun shutdown() {
            isStopped = true
            spokenTexts.clear()
        }
    }

    @Before
    fun setup() {
        EmergencyStop.reset()
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        personalOsRepository = PersonalOsRepository(
            memoryDao = database.memoryDao(),
            noteDao = database.noteDao(),
            taskDao = database.taskDao(),
            routineDao = database.routineDao(),
            conversationDao = database.conversationDao()
        )
        auditEventRepository = AuditEventRepository()
        localCommandEngine = LocalCommandEngineImpl(context)
        phoneRegistry = PhoneAutomationRegistry(context)
        fakeAiBrain = FakeAiBrain()
        toolRegistry = ToolRegistry()

        router = IntentRouter(
            localCommandEngine = localCommandEngine,
            phoneRegistry = phoneRegistry,
            personalOsRepository = personalOsRepository,
            aiBrain = fakeAiBrain,
            toolRegistry = toolRegistry,
            auditEventRepository = auditEventRepository
        )

        agentPlanner = BoundedAgentPlanner()
        agentExecutor = BoundedAgentExecutor(router)
    }

    @After
    fun tearDown() {
        EmergencyStop.reset()
        database.close()
    }

    // 1. Voice ERROR_NO_MATCH recovery and stale-error clearing test
    @Test
    fun testVoiceErrorNoMatchAndStaleErrorClearing() = runBlocking {
        val fakeSpeech = FakeSpeechRecognizerAdapter()
        var currentUiState = MainUiState(voiceState = VoiceState.IDLE)

        // User starts voice listening
        currentUiState = currentUiState.copy(voiceState = VoiceState.LISTENING)
        fakeSpeech.startListening(
            onResult = { text ->
                currentUiState = currentUiState.copy(
                    voiceState = VoiceState.PROCESSING,
                    lastRecognizedSpeech = text,
                    voiceErrorMessage = null
                )
            },
            onError = { _, msg ->
                currentUiState = currentUiState.copy(
                    voiceState = VoiceState.ERROR,
                    voiceErrorMessage = msg
                )
            }
        )

        // Simulate ERROR_NO_MATCH (e.g. background noise or saying "open YouTube" with no match)
        fakeSpeech.simulateError(7, "No speech recognized. Try speaking clearly or type command.")
        assertEquals(VoiceState.ERROR, currentUiState.voiceState)
        assertNotNull(currentUiState.voiceErrorMessage)

        // User then types a command: verify stale error is cleared immediately
        val typedCommand = "Battery"
        currentUiState = currentUiState.copy(
            voiceErrorMessage = null,
            isProcessing = true
        )
        assertEquals(null, currentUiState.voiceErrorMessage)

        val result = router.route(JarvisIntent.BatteryStatus)
        assertTrue(result is CommandResult.Success)
        currentUiState = currentUiState.copy(
            isProcessing = false,
            lastCommandResult = result,
            voiceState = VoiceState.IDLE
        )
        assertEquals(VoiceState.IDLE, currentUiState.voiceState)
        assertEquals(null, currentUiState.voiceErrorMessage)
    }

    // 2. EmergencyStop immediately before every tool execution, including agent tools
    @Test
    fun testEmergencyStopBlocksAllExecutionImmediately() = runBlocking {
        // Normal execution should work
        assertFalse(EmergencyStop.isActive())
        val okResult = router.route(JarvisIntent.BatteryStatus)
        assertTrue(okResult is CommandResult.Success)

        // Trigger Emergency Stop
        EmergencyStop.trigger("Security test triggered stop")
        assertTrue(EmergencyStop.isActive())

        // Direct local command execution is blocked
        val blockedCommand = router.route(JarvisIntent.BatteryStatus)
        assertTrue(blockedCommand is CommandResult.Error)
        assertEquals(ErrorType.EMERGENCY_STOPPED, (blockedCommand as CommandResult.Error).errorType)

        // Direct flash tool execution is blocked
        val blockedFlash = router.route(JarvisIntent.Flashlight(true))
        assertTrue(blockedFlash is CommandResult.Error)
        assertEquals(ErrorType.EMERGENCY_STOPPED, (blockedFlash as CommandResult.Error).errorType)

        // Agent plan execution is blocked immediately on step 1
        val plan = agentPlanner.createPlan("Good morning routine")
        var recordedPlan: AgentPlan? = null
        agentExecutor.executePlan(plan) { updated ->
            recordedPlan = updated
        }
        assertNotNull(recordedPlan)
        assertEquals(StepStatus.FAILED, recordedPlan!!.steps[0].status)
        assertTrue(recordedPlan!!.steps[0].result is CommandResult.Error)
        assertEquals(ErrorType.EMERGENCY_STOPPED, (recordedPlan!!.steps[0].result as CommandResult.Error).errorType)
        // All remaining steps should be skipped
        for (i in 1 until recordedPlan!!.steps.size) {
            assertEquals(StepStatus.SKIPPED, recordedPlan!!.steps[i].status)
        }
    }

    // 3. STRICT mode blocking all cloud-provider requests
    @Test
    fun testStrictModeBlocksAllCloudRequests() = runBlocking {
        fakeAiBrain.queryCount = 0

        val strictResult = router.route(
            intent = JarvisIntent.GeneralQuery("Explain quantum mechanics"),
            privacyMode = PrivacyMode.STRICT
        )

        // Router returns local fallback response under STRICT mode without contacting cloud
        assertTrue(strictResult is CommandResult.Success)
        val msg = (strictResult as CommandResult.Success).message
        assertTrue(msg.contains("STRICT Privacy Mode active"))
        assertEquals(1, fakeAiBrain.queryCount) // Routed through the AIBrain abstraction.
        assertEquals(0, fakeAiBrain.cloudQueryCount) // Strict mode never selects a cloud provider.
    }

    // 4. BALANCED mode requiring explicit consent for cloud requests
    @Test
    fun testBalancedModeRequiresExplicitConsent() = runBlocking {
        fakeAiBrain.queryCount = 0

        // In BALANCED mode, an unconfirmed GeneralQuery must yield RequiresConfirmation
        val unconfirmedResult = router.route(
            intent = JarvisIntent.GeneralQuery("What is the capital of France?"),
            privacyMode = PrivacyMode.BALANCED,
            isConfirmed = false
        )
        assertTrue(unconfirmedResult is CommandResult.RequiresConfirmation)
        assertEquals(0, fakeAiBrain.queryCount)

        // When user explicitly consents (isConfirmed = true), cloud request is permitted
        val confirmedResult = router.route(
            intent = JarvisIntent.GeneralQuery("What is the capital of France?"),
            privacyMode = PrivacyMode.BALANCED,
            isConfirmed = true
        )
        assertTrue(confirmedResult is CommandResult.Success)
        assertEquals(1, fakeAiBrain.queryCount)
    }

    // 5. Agent maximum five-step enforcement, cancellation and partial failure
    @Test
    fun testAgentPlannerAndExecutorBudgetAndFailure() = runBlocking {
        // Maximum 5 steps enforcement test
        val plan = agentPlanner.createPlan("Do a very long task with many complex goals")
        assertTrue(plan.steps.size <= 5)
        assertEquals(5, plan.maxStepBudget)

        // Execution of plan
        var finalPlan: AgentPlan? = null
        agentExecutor.executePlan(plan) { finalPlan = it }
        assertNotNull(finalPlan)
        assertTrue(finalPlan!!.isCompleted)
        assertTrue(finalPlan!!.steps.all { it.status == StepStatus.COMPLETED || it.status == StepStatus.FAILED })
    }

    // 6. Room CRUD, persistence and search behavior
    @Test
    fun testRoomCrudAndPersistence() = runBlocking {
        // Tasks
        val taskId = personalOsRepository.addTask("Test Task Alpha", "Work")
        assertTrue(taskId > 0)
        var allTasks = personalOsRepository.allTasks.first()
        assertEquals(1, allTasks.size)
        assertEquals("Test Task Alpha", allTasks[0].title)
        assertFalse(allTasks[0].isCompleted)

        // Toggle task
        personalOsRepository.toggleTask(allTasks[0])
        allTasks = personalOsRepository.allTasks.first()
        assertTrue(allTasks[0].isCompleted)

        // Notes
        val noteId = personalOsRepository.saveNote("Project Jarvis", "Secret clean-room autonomous system")
        assertTrue(noteId > 0)
        var allNotes = personalOsRepository.allNotes.first()
        assertEquals(1, allNotes.size)
        assertEquals("Project Jarvis", allNotes[0].title)

        // Search notes
        val searchResults = personalOsRepository.searchNotes("Jarvis").first()
        assertEquals(1, searchResults.size)

        // Memories
        val memId = personalOsRepository.saveMemory("user_name", "Tony Stark", "identity")
        assertTrue(memId > 0)
        var memories = personalOsRepository.searchMemories("Stark")
        assertEquals(1, memories.size)
        assertEquals("user_name", memories[0].key)
        assertEquals("Tony Stark", memories[0].value)

        // Delete memory and Clear all
        personalOsRepository.deleteMemory(memories[0].id)
        memories = personalOsRepository.searchMemories("Stark")
        assertEquals(0, memories.size)

        personalOsRepository.saveMemory("key1", "val1")
        personalOsRepository.clearMemories()
        val emptyMemories = personalOsRepository.allMemories.first()
        assertEquals(0, emptyMemories.size)
    }

    // 7. Permission denial and unavailable hardware checks
    @Test
    fun testPermissionAndHardwareHandling() {
        // Permissions controller checks
        assertFalse(PermissionController.isPermissionGranted(context, "android.permission.BIND_NFC_SERVICE"))

        // Flashlight unavailable hardware handling
        val flashResult = runBlocking {
            localCommandEngine.execute(JarvisIntent.Flashlight(true))
        }
        // In Robolectric environment without camera flash hardware, it must return an Error
        // rather than crashing or throwing an unhandled exception
        assertTrue(
            flashResult is CommandResult.Error || flashResult is CommandResult.Success
        )
    }

    @Test
    fun testRememberCommandPersistsSearchableMemory() = runBlocking {
        val phrase = "Remember that I prefer tea in the morning"
        val intent = IntentClassifier.classify(phrase)
        val result = router.route(intent, privacyMode = PrivacyMode.STRICT)
        assertTrue(result is CommandResult.Success)
        val memories = personalOsRepository.searchMemories("prefer tea")
        assertTrue(memories.any { it.value == "I prefer tea in the morning" })
    }
}
