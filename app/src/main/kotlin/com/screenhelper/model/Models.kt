package com.screenhelper.model

enum class Emotion(val label: String) {
    NEUTRAL("平静"), HAPPY("开心"), ANGRY("生气"), SAD("悲伤"), THINKING("思考");
    companion object {
        fun parse(value: String?) = entries.firstOrNull { it.name.equals(value?.trim(), true) } ?: NEUTRAL
    }
}

data class CharacterProfile(
    val id: String = "default",
    val name: String = "迷迷",
    val subtitle: String = "把相遇写进记忆，一起走过逐火之旅♪",
    val personality: String = "温柔、好奇，偶尔有一点俏皮。尊重对方的节奏，关心具体的小事。",
    val world: String = "星灯岛漂浮在云海之上，每一盏星灯都收藏着一个人的日常。",
    val backstory: String = "为了收集那些微小却闪亮的故事，小栖带着星灯来到你的桌面。",
    val assets: Map<Emotion, String> = emptyMap(),
)

data class ChatMessage(
    val id: String,
    val characterId: String,
    val role: String,
    val text: String,
    val createdAt: Long,
    val imagePath: String? = null,
    val status: String = "complete",
    val emotion: Emotion = Emotion.NEUTRAL,
)

data class RelationshipState(
    val characterId: String,
    val points: Int = 0,
    val activeSeconds: Long = 0,
    val dailyPoints: Int = 0,
    val day: String = "",
    val lastAwardAt: Long = 0,
    val lastInteractionAt: Long = 0,
) {
    val stage: String get() = when {
        points >= 140 && activeSeconds >= 7200 -> "心有默契"
        points >= 60 && activeSeconds >= 1800 -> "彼此信赖"
        points >= 20 && activeSeconds >= 300 -> "渐渐熟悉"
        else -> "初次相遇"
    }
    val expression: String get() = when (stage) {
        "心有默契" -> "可以自然表达关心，引用共同记忆；保持角色性格，尊重用户边界。"
        "彼此信赖" -> "可以主动分享想法、关心近况，并自然提起共同经历。"
        "渐渐熟悉" -> "语气放松一些，可以轻微打趣，保持克制和体贴。"
        else -> "礼貌、友好、不过分亲昵，通过具体的小事了解对方。"
    }
}

data class Memo(
    val id: String,
    val title: String,
    val dueAt: Long,
    val completed: Boolean = false,
    val version: Int = 1,
    val deliveredVersion: Int = 0,
    val timezone: String = java.time.ZoneId.systemDefault().id,
)

data class MemoryFact(val id: String, val characterId: String, val text: String, val createdAt: Long)
data class EndpointConfig(val baseUrl: String = "", val model: String = "", val key: String = "")
data class AppSettings(
    val demoMode: Boolean = true,
    val text: EndpointConfig = EndpointConfig("https://api.deepseek.com", "deepseek-chat"),
    val vision: EndpointConfig = EndpointConfig(),
    val speech: EndpointConfig = EndpointConfig(),
    val textSupportsImages: Boolean = false,
    val touchTalk: Boolean = true,
)

data class ModelTurn(val role: String, val text: String, val imagePath: String? = null, val imagePaths: List<String> = emptyList(), val imageUrls: List<String> = emptyList()) {
    val images get() = listOfNotNull(imagePath) + imagePaths
    val hasImages get() = images.isNotEmpty() || imageUrls.isNotEmpty()
}
data class ModelRequest(val system: String, val turns: List<ModelTurn>)
sealed interface AiEvent {
    data class Text(val value: String) : AiEvent
    data class Feeling(val emotion: Emotion) : AiEvent
}
data class ChatRuntime(
    val busy: Boolean = false,
    val characterId: String = "",
    val streamingText: String = "",
    val error: String? = null,
)
data class PetState(
    val character: CharacterProfile = CharacterProfile(),
    val emotion: Emotion = Emotion.NEUTRAL,
    val bubble: String = "今天，也一起慢慢来吧。",
    val bubbleId: Long = 0,
    val reminderId: String? = null,
)

data class DailyMemory(val characterId: String, val day: String, val summary: String, val sourceHash: String,
    val messageCount: Int, val archivePath: String, val generatedAt: Long, val timezone: String)

data class ScreenWatchState(val running: Boolean = false, val frames: Int = 0, val busy: Boolean = false,
    val message: String = "屏幕互动未开启")
data class CaptureResult(val path: String? = null, val error: String? = null)
