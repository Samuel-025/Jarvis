package com.example.core.agent

import com.example.core.model.CommandResult
import com.example.core.model.ErrorType
import com.example.core.router.IntentRouter
import com.example.core.safety.EmergencyStop
import kotlinx.coroutines.delay

class BoundedAgentExecutor(
    private val intentRouter: IntentRouter
) {
    suspend fun executePlan(
        plan: AgentPlan,
        onStepUpdate: (AgentPlan) -> Unit
    ): AgentPlan {
        var currentPlan = plan
        val updatedSteps = currentPlan.steps.toMutableList()

        for (i in updatedSteps.indices) {
            // Recheck EmergencyStop immediately before every step
            if (EmergencyStop.isActive()) {
                updatedSteps[i] = updatedSteps[i].copy(
                    status = StepStatus.FAILED,
                    result = CommandResult.Error(
                        message = "Agent execution halted by Emergency Stop.",
                        errorType = ErrorType.EMERGENCY_STOPPED
                    )
                )
                // Mark remaining as SKIPPED
                for (j in (i + 1) until updatedSteps.size) {
                    updatedSteps[j] = updatedSteps[j].copy(status = StepStatus.SKIPPED)
                }
                currentPlan = currentPlan.copy(steps = updatedSteps, isCompleted = true)
                onStepUpdate(currentPlan)
                return currentPlan
            }

            // Mark step RUNNING
            updatedSteps[i] = updatedSteps[i].copy(status = StepStatus.RUNNING)
            currentPlan = currentPlan.copy(steps = updatedSteps)
            onStepUpdate(currentPlan)

            delay(300) // Brief execution pause for realism and stability

            // Execute via IntentRouter (which enforces RiskEngine and Safety)
            val result = intentRouter.route(updatedSteps[i].intent, source = "AGENT")

            val stepStatus = when (result) {
                is CommandResult.Success -> StepStatus.COMPLETED
                is CommandResult.RequiresConfirmation -> StepStatus.FAILED // Agent cannot auto-confirm consequential actions
                is CommandResult.Error -> StepStatus.FAILED
            }

            updatedSteps[i] = updatedSteps[i].copy(status = stepStatus, result = result)
            currentPlan = currentPlan.copy(steps = updatedSteps)
            onStepUpdate(currentPlan)

            // If a critical step failed, gracefully stop subsequent dependent steps
            if (stepStatus == StepStatus.FAILED) {
                for (j in (i + 1) until updatedSteps.size) {
                    updatedSteps[j] = updatedSteps[j].copy(status = StepStatus.SKIPPED)
                }
                break
            }
        }

        currentPlan = currentPlan.copy(steps = updatedSteps, isCompleted = true)
        onStepUpdate(currentPlan)
        return currentPlan
    }
}
