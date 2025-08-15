package com.d4rk.cleaner.core.utils.helpers

import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.cleaner.core.data.datastore.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Helper that exposes flows related to the Clean Streak feature.
 */
class StreakCardHelper(
    private val dataStore: DataStore,
    private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
) {
    fun observeStreak(onUpdate: (Int, Int) -> Unit) {
        scope.launch(dispatchers.io) {
            combine(dataStore.streakCount, dataStore.streakRecord) { count, record ->
                count to record
            }.collect { (count, record) -> onUpdate(count, record) }
        }
    }

    fun observeStreakVisibility(onUpdate: (Boolean) -> Unit, onHideUntil: (Long) -> Unit) {
        scope.launch(dispatchers.io) {
            combine(dataStore.showStreakCard, dataStore.streakHideUntil) { show, hide ->
                onHideUntil(hide)
                show && hide <= System.currentTimeMillis()
            }.collect { visible -> onUpdate(visible) }
        }
    }
}
