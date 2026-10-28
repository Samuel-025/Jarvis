package com.example

import com.example.core.model.CommandResult
import com.example.core.model.JarvisIntent
import com.example.core.permissions.CapabilityPermissionBroker
import com.example.core.permissions.PermissionGrantChecker
import com.example.core.skills.JarvisSkill
import com.example.core.skills.SkillRegistry
import com.example.core.tools.Tool
import com.example.core.tools.ToolRegistry
import org.junit.Assert.*
import org.junit.Test

class CapabilityRegistryTest {
    private class BatteryTool(
        override val id: String = "device.battery",
        override val name: String = "Battery",
        private val enabled: Boolean = true
    ) : Tool {
        override val description = "Read battery status"
        override fun canHandle(intent: JarvisIntent) =
            enabled && intent is JarvisIntent.BatteryStatus
        override suspend fun execute(intent: JarvisIntent) =
            CommandResult.Success("Battery checked")
    }

    private class BatterySkill(override val id: String) : JarvisSkill {
        override val displayName = "Battery skill"
        override val description = "Explains battery information"
        override fun handles(intent: JarvisIntent) = intent is JarvisIntent.BatteryStatus
    }

    @Test fun toolRegistryRejectsDuplicateIdsAndPreservesOrder() {
        val registry = ToolRegistry()
        val first = BatteryTool()
        assertTrue(registry.register(first))
        assertFalse(registry.register(BatteryTool(id = "DEVICE.BATTERY")))
        assertEquals(listOf(first), registry.getAll())
        assertEquals(first, registry.getById("Device.Battery"))
    }

    @Test fun toolRegistryFindsHandlersAndCanUnregister() {
        val registry = ToolRegistry()
        val first = BatteryTool()
        val second = BatteryTool(id = "device.telemetry", name = "Telemetry")
        assertTrue(registry.register(first))
        assertTrue(registry.register(second))
        assertEquals(2, registry.findHandlers(JarvisIntent.BatteryStatus).size)
        assertEquals(first, registry.findHandler(JarvisIntent.BatteryStatus))
        assertTrue(registry.unregister("DEVICE.BATTERY"))
        assertNull(registry.getById("device.battery"))
        assertEquals(1, registry.getAll().size)
    }

    @Test fun toolRegistryRejectsBlankIds() {
        val registry = ToolRegistry()
        assertFalse(registry.register(BatteryTool(id = "  ")))
        assertTrue(registry.getAll().isEmpty())
    }

    @Test fun skillRegistryRejectsDuplicateIdsAndFindsHandlers() {
        val registry = SkillRegistry()
        val skill = BatterySkill("device.battery")
        assertTrue(registry.register(skill))
        assertFalse(registry.register(BatterySkill("DEVICE.BATTERY")))
        assertEquals(skill, registry.getById("Device.Battery"))
        assertEquals(listOf(skill), registry.findHandlers(JarvisIntent.BatteryStatus))
        assertTrue(registry.unregister("device.battery"))
        assertTrue(registry.getAllSkills().isEmpty())
    }

    @Test fun permissionBrokerSeparatesGrantedAndDeniedPermissions() {
        val broker = CapabilityPermissionBroker(
            PermissionGrantChecker { it == "android.permission.CAMERA" }
        )
        val decision = broker.evaluate(setOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.CAMERA",
            " ",
            "android.permission.CAMERA"
        ))
        assertFalse(decision.isAllowed)
        assertEquals(setOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO"), decision.requiredPermissions)
        assertEquals(setOf("android.permission.CAMERA"), decision.grantedPermissions)
        assertEquals(setOf("android.permission.RECORD_AUDIO"), decision.deniedPermissions)
    }

    @Test fun permissionBrokerAllowsCapabilitiesWithNoRuntimePermissions() {
        val decision = CapabilityPermissionBroker(PermissionGrantChecker { false }).evaluate(emptySet())
        assertTrue(decision.isAllowed)
        assertTrue(decision.requiredPermissions.isEmpty())
        assertTrue(decision.deniedPermissions.isEmpty())
    }
}
