package com.fmaestre98.pdfviewer.sdk.core.overlay

import android.os.SystemClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SelectionDragManager(private val lookupIntervalMs: Long = 8L) {
    private var lastLookup = 0L
    private val mutex = Mutex()

    suspend fun shouldLookup(): Boolean {
        return mutex.withLock {
            val now = SystemClock.elapsedRealtime()
            if (now - lastLookup >= lookupIntervalMs) {
                lastLookup = now
                true
            } else false
        }
    }

    fun reset() {
        lastLookup = 0L
    }
}
