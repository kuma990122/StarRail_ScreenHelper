package com.screenhelper.business.pet

import com.screenhelper.model.CharacterProfile

/** Local greetings derived from assets/skills/mimi.md; tapping never calls an API. */
object TouchReplyPolicy {
    fun replies(character: CharacterProfile, waking: Boolean): List<String> = when {
        waking -> listOf("醒来啦，人家在听♪", "有你在，梦也变得可爱啦♪")
        character.id == "default" -> listOf(
            "我在呢，一起写下新故事吧♪", "这份小小的浪漫，人家记住啦♪",
            "被你发现啦，可爱的伙伴♪", "今天的记忆，也想和你分享♪",
            "再难的旅途，人家也陪着你♪", "轻轻碰一下，故事就开始啦♪",
        )
        else -> listOf("${character.name.take(8)}在这里陪你。", "今天有什么小故事想分享？")
    }
}
