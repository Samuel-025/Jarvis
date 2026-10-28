package com.example.core.permissions

/** Platform-neutral permission check used at the capability execution boundary. */
fun interface PermissionGrantChecker {
    fun isGranted(permission: String): Boolean
}

data class CapabilityPermissionDecision(
    val requiredPermissions: Set<String>,
    val grantedPermissions: Set<String>,
    val deniedPermissions: Set<String>
) {
    val isAllowed: Boolean get() = deniedPermissions.isEmpty()
}

/**
 * Centralizes current permission-grant evaluation.
 * It never requests permissions; prompts must be initiated by visible, user-driven Android UI.
 */
class CapabilityPermissionBroker(
    private val grantChecker: PermissionGrantChecker
) {
    fun evaluate(requiredPermissions: Set<String>): CapabilityPermissionDecision {
        val normalized = requiredPermissions.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSortedSet()
        val granted = linkedSetOf<String>()
        val denied = linkedSetOf<String>()

        normalized.forEach { permission ->
            if (grantChecker.isGranted(permission)) granted += permission
            else denied += permission
        }

        return CapabilityPermissionDecision(normalized, granted, denied)
    }
}
