package com.d4rk.cleaner.app.clean.scanner.ui

import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.utils.helpers.StreakCardHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Manages flows and actions related to the clean streak card.
 */
class StreakHandler(
    private val dataStore: DataStore,
    private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val updateHideDialogVisibility: (Boolean) -> Unit,
) {
    private val streakHelper = StreakCardHelper(dataStore, scope, dispatchers)

    private val _cleanStreak = MutableStateFlow(0)
    val cleanStreak: StateFlow<Int> = _cleanStreak

    private val _streakRecord = MutableStateFlow(0)
    val streakRecord: StateFlow<Int> = _streakRecord

    private val _showStreakCard = MutableStateFlow(true)
    val showStreakCard: StateFlow<Boolean> = _showStreakCard
    private val _streakHideUntil = MutableStateFlow(0L)
    val streakHideUntil: StateFlow<Long> = _streakHideUntil

    init {
        loadStreakStats()
        loadStreakCardVisibility()
    }

    fun setHideStreakDialogVisibility(isVisible: Boolean) {
        updateHideDialogVisibility(isVisible)
    }

    fun hideStreakForNow() {
        scope.launch(dispatchers.io) { dataStore.saveStreakHideUntil(startOfNextWeek()) }
        setHideStreakDialogVisibility(false)
    }

    fun hideStreakPermanently() {
        scope.launch(dispatchers.io) { dataStore.saveShowStreakCard(false) }
        setHideStreakDialogVisibility(false)
    }

    private fun loadStreakStats() {
        streakHelper.observeStreak { current, record ->
            _cleanStreak.value = current
            _streakRecord.value = record
        }
    }

    private fun loadStreakCardVisibility() {
        streakHelper.observeStreakVisibility(
            onUpdate = { _showStreakCard.value = it },
            onHideUntil = { _streakHideUntil.value = it }
        )
    }

    private fun startOfNextWeek(): Long {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            java.time.LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } else {
            val cal = java.util.Calendar.getInstance()
            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            var daysUntilMonday = (java.util.Calendar.MONDAY - dayOfWeek + 7) % 7
            if (daysUntilMonday == 0) daysUntilMonday = 7
            cal.add(java.util.Calendar.DAY_OF_YEAR, daysUntilMonday)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
    }
}
