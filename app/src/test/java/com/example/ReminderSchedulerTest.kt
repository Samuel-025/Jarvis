package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.phone.ReminderScheduler
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReminderSchedulerTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun rejectsZeroAndNegativeDelays() {
        assertFalse(ReminderScheduler.schedule(context, 0L, "Drink water"))
        assertFalse(ReminderScheduler.schedule(context, -1L, "Drink water"))
    }

    @Test
    fun rejectsDelayBeyondThirtyDays() {
        val thirtyDaysAndOneMillisecond = 30L * 24L * 60L * 60L * 1000L + 1L
        assertFalse(ReminderScheduler.schedule(context, thirtyDaysAndOneMillisecond, "Drink water"))
    }

    @Test
    fun rejectsBlankReminderTitles() {
        assertFalse(ReminderScheduler.schedule(context, 60_000L, "   "))
    }

    @Test
    fun acceptsValidLocalReminder() {
        assertTrue(ReminderScheduler.schedule(context, 60_000L, "Drink water"))
    }
}
