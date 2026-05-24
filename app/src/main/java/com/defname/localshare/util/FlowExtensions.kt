// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Represents a change in a list emitted by a Flow.
 */
sealed class FlowDelta<T>(val obj: T) {
    class Added<T>(obj: T) : FlowDelta<T>(obj)
    class Removed<T>(obj: T) : FlowDelta<T>(obj)
}

/**
 * Transforms a Flow of lists into a Flow of delta events (Added/Removed).
 * 
 * @param key A function to extract a unique key from the object for comparison.
 */
fun <T, K> Flow<List<T>>.asDeltaEvents(key: (T) -> K): Flow<FlowDelta<T>> = flow {
    var oldList = emptyList<T>()
    this@asDeltaEvents.collect { newList ->
        // 1. What is new? (In newList, but not in oldList)
        val added = newList.filter { newItem -> oldList.none { key(it) == key(newItem) } }
        added.forEach { emit(FlowDelta.Added(it)) }

        // 2. What was removed? (In oldList, but not in newList)
        val removed = oldList.filter { oldItem -> newList.none { key(it) == key(oldItem) } }
        removed.forEach { emit(FlowDelta.Removed(it)) }

        oldList = newList
    }
}
