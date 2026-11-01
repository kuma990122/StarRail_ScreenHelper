package com.screenhelper.business.pet

import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

enum class PetAction { IDLE, CLICK, DRAG, WAKE, SLEEP, REMINDER, LEFT, RIGHT, BOTTOM }
data class PetSize(val width: Int, val height: Int)

object PetPolicy {
    fun sleeping(hour: Int, idleMillis: Long) = idleMillis >= 30_000 && (hour in 12..13 || hour >= 21 || hour < 7)
    fun greetingAllowed(now: Long, last: Long?) = last == null || now - last >= 10_000
    fun dock(x: Int, y: Int, maxX: Int, maxY: Int, threshold: Int): PetAction = when {
        y >= maxY - threshold -> PetAction.BOTTOM
        x <= threshold -> PetAction.LEFT
        x >= maxX - threshold -> PetAction.RIGHT
        else -> PetAction.IDLE
    }
    fun overlayWidth(screenWidth: Int) = (screenWidth * .32f).toInt().coerceAtLeast(1)
    fun windowSize(screenWidth: Int, screenHeight: Int, inApp: Boolean, bottom: Boolean = false): PetSize {
        if (inApp) return PetSize((screenWidth * .15f).toInt().coerceAtLeast(1), (screenHeight * .15f).toInt().coerceAtLeast(1))
        val side = (screenWidth * .32f * if(bottom) .7f else 1f).toInt().coerceAtLeast(1)
        return PetSize(side, side)
    }
    fun bubbleSize(screenWidth: Int, bottom: Boolean = false): PetSize {
        val width = (screenWidth * .4f * .8f * .6f * if(bottom) .7f else 1f).toInt().coerceAtLeast(1)
        return PetSize(width, (width / 2).coerceAtLeast(1))
    }
    fun nextMemo(memos: List<com.screenhelper.model.Memo>) = memos.filterNot { it.completed }
        .minWithOrNull(compareBy<com.screenhelper.model.Memo> { it.dueAt }.thenBy { it.id })
    fun weekSeconds(now: ZonedDateTime): Long {
        val start = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(now.zone)
        return java.time.Duration.between(start, now).seconds.coerceAtLeast(0)
    }
}

/** Owns at most three chronological snapshots. Caller handles disposing evicted frames. */
class FrameWindow<T>(private val capacity: Int = 3) {
    init { require(capacity > 0) }
    private val values = ArrayDeque<T>()
    @Synchronized fun add(value: T): T? {
        val old = if (values.size == capacity) values.removeFirst() else null
        values.addLast(value)
        return old
    }
    @Synchronized fun snapshot(): List<T> = values.toList()
    @Synchronized fun clear(): List<T> = values.toList().also { values.clear() }
}
