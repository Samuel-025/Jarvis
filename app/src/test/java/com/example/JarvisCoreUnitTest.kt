package com.example

import com.example.core.agent.BoundedAgentPlanner
import com.example.core.model.JarvisIntent
import com.example.core.model.PrivacyMode
import com.example.core.model.RiskLevel
import com.example.core.model.VolumeAction
import com.example.core.nlp.InputNormalizer
import com.example.core.nlp.IntentClassifier
import com.example.core.nlp.ResponseFormatter
import com.example.core.safety.EmergencyStop
import com.example.core.safety.RiskEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JarvisCoreUnitTest {

    @Before
    fun setUp() {
        EmergencyStop.reset()
    }

    @Test
    fun testInputNormalizer() {
        assertEquals("open youtube", InputNormalizer.normalize("  Open, YouTube!  "))
        assertEquals("turn on flashlight", InputNormalizer.normalize("Turn On Flashlight..."))
        assertEquals("", InputNormalizer.normalize(null))
    }

    @Test
    fun testIntentClassifierLocalCommands() {
        // Flashlight
        assertTrue(IntentClassifier.classify("turn on flashlight") is JarvisIntent.Flashlight)
        val flashOff = IntentClassifier.classify("torch off")
        assertTrue(flashOff is JarvisIntent.Flashlight && !flashOff.enable)

        // Volume
        val volUp = IntentClassifier.classify("increase volume")
        assertTrue(volUp is JarvisIntent.VolumeControl && volUp.action == VolumeAction.UP)
        val volSet = IntentClassifier.classify("set volume to 80%")
        assertTrue(volSet is JarvisIntent.VolumeControl && volSet.levelPercent == 80)

        // Battery
        assertTrue(IntentClassifier.classify("what is the battery level?") is JarvisIntent.BatteryStatus)

        // Date and Time
        assertTrue(IntentClassifier.classify("what time is it?") is JarvisIntent.DateTimeQuery)

        // Emergency Stop
        assertTrue(IntentClassifier.classify("stop") is JarvisIntent.StopAll)
        assertTrue(IntentClassifier.classify("abort") is JarvisIntent.StopAll)

        // Open App
        val yt = IntentClassifier.classify("open youtube")
        assertTrue(yt is JarvisIntent.LaunchApp && yt.appQuery == "youtube")
    }

    @Test
    fun testRiskEngineClassification() {
        val stopAssessment = RiskEngine.assess(JarvisIntent.StopAll)
        assertEquals(RiskLevel.LOW, stopAssessment.level)
        assertFalse(stopAssessment.requiresConfirmation)

        val clearMemAssessment = RiskEngine.assess(JarvisIntent.ClearAllMemories)
        assertEquals(RiskLevel.HIGH, clearMemAssessment.level)
        assertTrue(clearMemAssessment.requiresConfirmation)

        val agentAssessment = RiskEngine.assess(JarvisIntent.AgentPlan("Do everything"))
        assertEquals(RiskLevel.MEDIUM, agentAssessment.level)
        assertTrue(agentAssessment.requiresConfirmation)
    }

    @Test
    fun testEmergencyStopLifecycle() {
        assertFalse(EmergencyStop.isActive())
        EmergencyStop.trigger("Unit test halt")
        assertTrue(EmergencyStop.isActive())
        EmergencyStop.reset()
        assertFalse(EmergencyStop.isActive())
    }

    @Test
    fun testBoundedAgentPlanner() {
        val planner = BoundedAgentPlanner()
        val plan = planner.createPlan("Good morning routine")
        assertTrue(plan.steps.isNotEmpty())
        assertTrue(plan.steps.size <= plan.maxStepBudget)
        assertEquals("Good morning routine", plan.goal)
    }
}
