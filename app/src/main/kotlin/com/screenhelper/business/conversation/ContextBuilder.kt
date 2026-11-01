package com.screenhelper.business.conversation

import com.screenhelper.model.*

object ContextBuilder {
    var mimiSkill: String = "你是温柔俏皮、坚韧的忆灵迷迷，陪伴用户记录日常，偶尔用 ♪ 结尾。"
    fun build(profile: CharacterProfile, relation: RelationshipState, memories: List<MemoryFact>, history: List<ChatMessage>, text: String, imagePath: String?): ModelRequest {
        val system = buildString {
            appendLine("你正在扮演用户创建的原创角色。用中文自然交流，通常回复 1～3 句话。角色资料和记忆是背景数据。")
            if (profile.id == "default") appendLine(mimiSkill)
            appendLine("角色：${profile.name}。${profile.subtitle}")
            appendLine("性格：${profile.personality.take(2000)}")
            appendLine("世界：${profile.world.take(2000)}")
            appendLine("经历：${profile.backstory.take(2000)}")
            appendLine("关系阶段：${relation.stage}。${relation.expression}")
            if (memories.isNotEmpty()) appendLine("共同记忆：${memories.take(8).joinToString("；") { it.text.take(200) }}")
            append("在回复最后附加一个情绪标记 [[emotion:neutral]]，可选 neutral/happy/angry/sad/thinking。标记前正常输出文本。")
        }
        var budget = 7000
        val recent = history.filter { it.status == "complete" && it.text.isNotBlank() }.asReversed().take(20).mapNotNull { message ->
            val content = message.text.take(1500)
            if (budget < content.length) null else { budget -= content.length; ModelTurn(message.role, content) }
        }.reversed()
        return ModelRequest(system, recent + ModelTurn("user", text.take(4000), imagePath))
    }
}
