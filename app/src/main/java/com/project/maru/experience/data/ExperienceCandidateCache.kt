package com.project.maru.experience.data

import com.project.maru.experience.Experience

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object ExperienceCandidateCache {
    private val snapshots = MutableStateFlow<List<Experience>>(emptyList())
    val candidates = snapshots.asStateFlow()
    var completed: List<Experience>? = null
        private set

    fun seed(events: List<Experience>) {
        snapshots.compareAndSet(emptyList(), events.toList())
    }

    fun publish(events: List<Experience>, complete: Boolean) {
        val snapshot = events.toList()
        if (complete) completed = snapshot
        snapshots.value = snapshot
    }
}
