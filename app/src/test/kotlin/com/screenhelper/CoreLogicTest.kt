package com.screenhelper

import com.screenhelper.business.conversation.ContextBuilder
import com.screenhelper.business.memo.MemoPolicy
import com.screenhelper.business.relationship.RelationshipPolicy
import com.screenhelper.data.remote.dto.*
import com.screenhelper.model.*
import org.junit.Assert.*
import org.junit.Test

class CoreLogicTest {
    @Test fun emotionSuffixSurvivesEveryChunkBoundary() {
        val full = "你好，一起加油！[[emotion:happy]]"
        for (split in 0..full.length) {
            val decoder = ReplyDecoder()
            val visible = decoder.append(full.take(split)) + decoder.append(full.drop(split))
            val (tail, emotion) = decoder.finish()
            assertEquals("你好，一起加油！", visible + tail)
            assertEquals(Emotion.HAPPY, emotion)
        }
    }
    @Test fun malformedEmotionFallsBackAndPartialMarkerDoesNotEatOrdinaryText() {
        val decoder = ReplyDecoder()
        assertEquals("hello", decoder.append("hello[[emo"))
        assertEquals("[[emo", decoder.finish().first)
        assertEquals(Emotion.NEUTRAL, Emotion.parse("unknown"))
    }
    @Test fun sseJoinsDataLinesAndIgnoresComments() {
        val decoder = SseDecoder()
        assertNull(decoder.line(": heartbeat"))
        assertNull(decoder.line("data: one"))
        assertNull(decoder.line("data: two"))
        assertEquals("one\ntwo", decoder.line(""))
        assertNull(decoder.flush())
    }
    @Test fun interactionCooldownDailyCapAndNextDayReset() {
        var state = RelationshipPolicy.interaction(RelationshipState("a"), 1000, "2026-10-19", 2)
        assertEquals(2, state.points)
        state = RelationshipPolicy.interaction(state, 2000, state.day, 2)
        assertEquals(2, state.points)
        for (i in 1..30) state = RelationshipPolicy.interaction(state, 1000L + i * 60000L, state.day, 2)
        assertEquals(24, state.points)
        state = RelationshipPolicy.interaction(state, 86_400_000, "2026-10-20", 2)
        assertEquals(26, state.points)
        assertEquals(2, state.dailyPoints)
    }
    @Test fun activeTimeRequiresRecentInteractionAndIsBounded() {
        val state = RelationshipState("a", lastInteractionAt = 1000)
        assertEquals(60L, RelationshipPolicy.active(state, 2000, 500).activeSeconds)
        assertEquals(0L, RelationshipPolicy.active(state, 122000, 30).activeSeconds)
        assertEquals(0L, RelationshipPolicy.active(state, 999, 30).activeSeconds)
        assertEquals("初次相遇", state.copy(points = 140).stage)
        assertEquals("心有默契", state.copy(points = 140, activeSeconds = 7200).stage)
    }
    @Test fun remindersRejectStaleCompletedEarlyOrAlreadyDeliveredEvents() {
        val memo = Memo("a", "drink", 1000, version = 2)
        assertTrue(MemoPolicy.valid(memo, 2, 1000))
        assertFalse(MemoPolicy.valid(memo, 1, 1000))
        assertFalse(MemoPolicy.valid(memo, 2, 999))
        assertFalse(MemoPolicy.valid(memo.copy(completed = true), 2, 1000))
        assertFalse(MemoPolicy.valid(memo.copy(deliveredVersion = 2), 2, 1000))
    }
    @Test(expected = IllegalArgumentException::class) fun pastReminderRejected() { MemoPolicy.validate("plan", 1000, 1000) }
    @Test(expected = IllegalArgumentException::class) fun blankReminderRejected() { MemoPolicy.validate("  ", 2000, 1000) }
    @Test fun promptContainsRoleRelationMemoryAndBoundedHistory() {
        val history = (0..39).map { ChatMessage("$it", "a", if (it % 2 == 0) "user" else "assistant", "x".repeat(1600), it.toLong()) }
        val request = ContextBuilder.build(CharacterProfile(name = "测试角色"), RelationshipState("a"), listOf(MemoryFact("m", "a", "喜欢茶", 1)), history, "hello", "image.png")
        assertTrue(request.system.contains("测试角色"))
        assertTrue(request.system.contains("喜欢茶"))
        assertTrue(request.turns.dropLast(1).sumOf { it.text.length } <= 7000)
        assertEquals("hello", request.turns.last().text)
        assertEquals("image.png", request.turns.last().imagePath)
    }
}
