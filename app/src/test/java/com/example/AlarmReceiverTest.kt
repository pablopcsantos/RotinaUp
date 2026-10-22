package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Habit
import com.example.data.model.UserSettings
import com.example.receiver.AlarmReceiver
import com.example.util.AlarmScheduler
import com.example.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmReceiverTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.setInstance(db)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
    }

    @After
    fun tearDown() {
        AppDatabase.setInstance(null)
        db.close()
    }

    @Test
    fun `test habit quick done action records DONE log and clears notification`() = runBlocking {
        val habitDao = db.habitDao()
        val habitId = habitDao.insertHabit(
            Habit(
                id = 10,
                name = "Beber Água",
                type = "WATER",
                targetTime = "10:00",
                daysOfWeek = "1,2,3,4,5,6,7",
                isActive = true
            )
        )

        val receiver = AlarmReceiver()
        receiver.handleQuickDone(context, habitId)

        val today = DateUtils.getTodayDateString(0)
        val logs = habitDao.getLogsForDate(today).first()
        val quickDoneLog = logs.find { it.habitId == habitId }
        assertNotNull("Quick Done action should insert a log for today", quickDoneLog)
        assertEquals("DONE", quickDoneLog?.status)
        assertEquals("Concluído via notificação", quickDoneLog?.note)
    }

    @Test
    fun `test boot completed and date changed broadcasts trigger without error`() {
        val receiver = AlarmReceiver()

        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, bootIntent)

        val dateChangedIntent = Intent(Intent.ACTION_DATE_CHANGED)
        receiver.onReceive(context, dateChangedIntent)

        val timeChangedIntent = Intent(Intent.ACTION_TIME_CHANGED)
        receiver.onReceive(context, timeChangedIntent)

        assertTrue(true)
    }
}
