package com.example.core.phone

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import com.example.core.model.CommandResult
import com.example.core.model.ErrorType
import com.example.core.model.RiskLevel

data class PhoneActionDescriptor(
    val id: String,
    val title: String,
    val description: String,
    val riskLevel: RiskLevel,
    val requiredPermission: String? = null
)

class PhoneAutomationRegistry(
    private val context: Context
) {
    fun getSupportedActions(): List<PhoneActionDescriptor> = listOf(
        PhoneActionDescriptor(
            id = "dial_phone",
            title = "Open Phone Dialer",
            description = "Opens system phone dialer with specified number",
            riskLevel = RiskLevel.LOW
        ),
        PhoneActionDescriptor(
            id = "send_sms_draft",
            title = "Draft SMS Message",
            description = "Opens SMS app with recipient and message filled in",
            riskLevel = RiskLevel.LOW
        ),
        PhoneActionDescriptor(
            id = "set_timer",
            title = "Set Timer",
            description = "Configures a countdown timer in Clock app",
            riskLevel = RiskLevel.LOW
        ),
        PhoneActionDescriptor(
            id = "open_web_url",
            title = "Open Web URL",
            description = "Opens a verified web URL in default browser",
            riskLevel = RiskLevel.LOW
        )
    )

    fun executeAction(actionId: String, params: Map<String, String>): CommandResult {
        return try {
            when (actionId) {
                "dial_phone" -> {
                    val number = params["number"] ?: ""
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    CommandResult.Success("Opened dialer for number $number", audioFeedback = "Dialer opened.")
                }
                "send_sms_draft" -> {
                    val number = params["number"] ?: ""
                    val message = params["message"] ?: ""
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$number")).apply {
                        putExtra("sms_body", message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    CommandResult.Success("Opened SMS composer for $number", audioFeedback = "SMS composer opened.")
                }
                "set_timer" -> {
                    val seconds = params["seconds"]?.toIntOrNull() ?: 60
                    val message = params["message"] ?: "JARVIS Timer"
                    val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                        putExtra(AlarmClock.EXTRA_MESSAGE, message)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        CommandResult.Success("Set timer for $seconds seconds", audioFeedback = "Timer set for $seconds seconds.")
                    } else {
                        CommandResult.Error("Clock application not available to set timer", ErrorType.APP_NOT_FOUND)
                    }
                }
                "open_web_url" -> {
                    val url = params["url"] ?: "https://www.google.com"
                    val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    CommandResult.Success("Opened URL: $formattedUrl", audioFeedback = "Opening link.")
                }
                else -> CommandResult.Error(
                    message = "Unsupported phone action: $actionId",
                    errorType = ErrorType.INVALID_INPUT
                )
            }
        } catch (e: Exception) {
            CommandResult.Error(
                message = "Phone action execution failed: ${e.message}",
                errorType = ErrorType.EXECUTION_FAILED
            )
        }
    }
}
