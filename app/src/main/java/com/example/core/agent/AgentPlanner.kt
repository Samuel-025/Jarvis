package com.example.core.agent

import com.example.core.model.CommandResult
import com.example.core.model.JarvisIntent

enum class StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}

data class AgentStep(
    val id: Int,
    val description: String,
    val intent: JarvisIntent,
    val status: StepStatus = StepStatus.PENDING,
    val result: CommandResult? = null
)

data class AgentPlan(
    val goal: String,
    val steps: List<AgentStep>,
    val maxStepBudget: Int = 5,
    val isCompleted: Boolean = false
)

interface AgentPlanner {
    fun createPlan(goal: String): AgentPlan
}

class BoundedAgentPlanner : AgentPlanner {
    override fun createPlan(goal: String): AgentPlan {
        val lowerGoal = goal.lowercase()
        val steps = mutableListOf<AgentStep>()

        when {
            lowerGoal.contains("morning routine") || lowerGoal.contains("good morning") -> {
                steps.add(AgentStep(1, "Check device battery level", JarvisIntent.BatteryStatus))
                steps.add(AgentStep(2, "Check current date and time", JarvisIntent.DateTimeQuery))
                steps.add(AgentStep(3, "Set music volume to 60%", JarvisIntent.VolumeControl(com.example.core.model.VolumeAction.SET_LEVEL, 60)))
                steps.add(AgentStep(4, "Add reminder task for the day", JarvisIntent.CreateTask("Review morning schedule", "Routine")))
            }
            lowerGoal.contains("meeting mode") || lowerGoal.contains("silent") -> {
                steps.add(AgentStep(1, "Turn off flashlight if on", JarvisIntent.Flashlight(false)))
                steps.add(AgentStep(2, "Mute media volume", JarvisIntent.VolumeControl(com.example.core.model.VolumeAction.MUTE)))
                steps.add(AgentStep(3, "Log meeting note", JarvisIntent.CreateNote("Meeting Mode Activated", "System set to quiet")))
            }
            lowerGoal.contains("night") || lowerGoal.contains("bedtime") -> {
                steps.add(AgentStep(1, "Turn off flashlight", JarvisIntent.Flashlight(false)))
                steps.add(AgentStep(2, "Mute device audio", JarvisIntent.VolumeControl(com.example.core.model.VolumeAction.MUTE)))
                steps.add(AgentStep(3, "Create evening reflection note", JarvisIntent.CreateNote("Evening Routine", "Day complete, devices silent")))
            }
            else -> {
                // Generic single/two-step plan bounded by max budget
                steps.add(AgentStep(1, "Assess device status", JarvisIntent.BatteryStatus))
                steps.add(AgentStep(2, "Record goal in notes", JarvisIntent.CreateNote("Agent Task: $goal", "Executed autonomously by JARVIS Mobile")))
            }
        }

        return AgentPlan(
            goal = goal,
            steps = steps.take(5), // strictly enforce step budget <= 5
            maxStepBudget = 5
        )
    }
}
