package com.d4rk.cleaner.core.utils.helpers

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object CleaningEventBus {
    /**
     * Emits the result of a cleaning job.
     * `true` when the job finished successfully, `false` otherwise.
     */
    private val _events = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val events: SharedFlow<Boolean> = _events.asSharedFlow()

    fun notifyCleaned(success: Boolean) {
        _events.tryEmit(success)
    }
}
