package com.example.core.skills

import com.example.core.model.JarvisIntent

interface JarvisSkill {
    val id: String
    val displayName: String
    val description: String
    fun handles(intent: JarvisIntent): Boolean
}

class SkillRegistry {
    private val skills = mutableListOf<JarvisSkill>()

    fun register(skill: JarvisSkill) {
        skills.add(skill)
    }

    fun getAllSkills(): List<JarvisSkill> = skills.toList()
}
