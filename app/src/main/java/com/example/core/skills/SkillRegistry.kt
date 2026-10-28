package com.example.core.skills

import com.example.core.model.JarvisIntent

interface JarvisSkill {
    val id: String
    val displayName: String
    val description: String
    fun handles(intent: JarvisIntent): Boolean
}

/** Thread-safe skill catalogue. It describes capabilities; it does not execute or grant permissions. */
class SkillRegistry {
    private val lock = Any()
    private val skillsById = linkedMapOf<String, JarvisSkill>()

    /** Returns true when registered; false for a blank or duplicate ID. */
    fun register(skill: JarvisSkill): Boolean = synchronized(lock) {
        val key = skill.id.trim().lowercase()
        if (key.isBlank() || skillsById.containsKey(key)) false
        else {
            skillsById[key] = skill
            true
        }
    }

    fun getAllSkills(): List<JarvisSkill> = synchronized(lock) {
        skillsById.values.toList()
    }

    fun getById(id: String): JarvisSkill? = synchronized(lock) {
        skillsById[id.trim().lowercase()]
    }

    fun findHandlers(intent: JarvisIntent): List<JarvisSkill> = synchronized(lock) {
        skillsById.values.filter { it.handles(intent) }
    }

    fun unregister(id: String): Boolean = synchronized(lock) {
        skillsById.remove(id.trim().lowercase()) != null
    }

    fun clear() = synchronized(lock) { skillsById.clear() }
}
