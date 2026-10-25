package com.screenhelper.business.memo

import com.screenhelper.data.platform.alarm.ReminderScheduler
import com.screenhelper.data.repository.LocalRepository
import com.screenhelper.model.Memo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

object MemoPolicy {
    fun validate(title: String, dueAt: Long, now: Long) {
        require(title.isNotBlank()) { "写下要做的事情吧" }
        require(title.trim().length <= 120) { "待办请控制在 120 字以内" }
        require(dueAt > now) { "提醒时间需要晚于现在" }
    }
    fun valid(memo: Memo, version: Int, now: Long): Boolean = !memo.completed && memo.version == version && memo.deliveredVersion < version && memo.dueAt <= now
}

class MemoService(
    private val local: LocalRepository,
    private val scheduler: ReminderScheduler,
    private val onReminder: (Memo) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    suspend fun save(title: String, dueAt: Long, id: String? = null): Memo = mutex.withLock {
        MemoPolicy.validate(title, dueAt, clock())
        val old = id?.let { local.memo(it) }
        val next = Memo(id ?: UUID.randomUUID().toString(), title.trim(), dueAt, false, (old?.version ?: 0) + 1, old?.deliveredVersion ?: 0)
        local.saveMemo(next)
        scheduler.cancel(next.id)
        scheduler.schedule(next)
        next
    }
    suspend fun toggle(id: String) = mutex.withLock {
        val current = local.memo(id) ?: return@withLock
        val next = current.copy(completed = !current.completed, version = current.version + 1)
        local.saveMemo(next)
        scheduler.cancel(id)
        if (!next.completed) scheduler.schedule(next)
    }
    suspend fun delete(id: String) = mutex.withLock { local.deleteMemo(id); scheduler.cancel(id) }
    suspend fun due(id: String, version: Int) {
        val current = local.memo(id) ?: return
        if (!MemoPolicy.valid(current, version, clock())) return
        val claimed = local.claimReminder(id, version, clock()) ?: return
        onReminder(claimed)
    }
    suspend fun restore() { local.pendingMemos().forEach { scheduler.schedule(it) } }
}
