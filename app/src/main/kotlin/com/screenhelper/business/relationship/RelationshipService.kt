package com.screenhelper.business.relationship

import com.screenhelper.data.repository.LocalRepository
import com.screenhelper.model.RelationshipState
import java.time.Instant
import java.time.ZoneId

object RelationshipPolicy {
    const val DAILY_LIMIT = 24
    fun interaction(state: RelationshipState, now: Long, day: String, weight: Int): RelationshipState {
        val daily = if (state.day == day) state.dailyPoints else 0
        val eligible = state.lastAwardAt == 0L || now - state.lastAwardAt >= 60_000
        val award = if (eligible) weight.coerceIn(0, DAILY_LIMIT - daily) else 0
        return state.copy(points = state.points + award, dailyPoints = daily + award, day = day,
            lastAwardAt = if (award > 0) now else state.lastAwardAt, lastInteractionAt = now)
    }
    fun active(state: RelationshipState, now: Long, seconds: Long): RelationshipState {
        if (state.lastInteractionAt == 0L || now - state.lastInteractionAt !in 0..120_000) return state
        return state.copy(activeSeconds = state.activeSeconds + seconds.coerceIn(0, 60))
    }
}

class RelationshipService(private val local: LocalRepository, private val clock: () -> Long = System::currentTimeMillis) {
    suspend fun interact(id: String, eventId: String, weight: Int = 2): RelationshipState {
        val now = clock()
        val day = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        return local.updateRelationship(eventId, now, id) { RelationshipPolicy.interaction(it, now, day, weight) }
    }
    suspend fun active(id: String, eventId: String, seconds: Long) {
        val now = clock()
        local.updateRelationship(eventId, now, id) { RelationshipPolicy.active(it, now, seconds) }
    }
}
