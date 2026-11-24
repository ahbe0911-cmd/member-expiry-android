package ir.memberexpiry.manager.core.date

import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/** Uses the device's current timezone unless an explicit clock is injected. */
class DateProvider(private val clock: Clock? = null) {
    fun nowMillis(): Long = clock?.millis() ?: System.currentTimeMillis()
    fun today(): JalaliDate = JalaliDate.fromGregorian(LocalDate.now(clock ?: Clock.systemDefaultZone()))
    fun observeToday(): Flow<JalaliDate> = flow {
        while (true) {
            emit(today())
            delay(15_000)
        }
    }.distinctUntilChanged()
}
