package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*

@Database(
    entities = [
        Habito::class,
        HabitLog::class,
        SleepLog::class,
        NapLog::class,
        DailyReview::class,
        UserSettings::class,
        AlarmEvent::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitoDao
    abstract fun sleepDao(): SleepDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS alarm_events (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "habitId INTEGER NOT NULL, " +
                    "habitName TEXT NOT NULL, " +
                    "scheduledAt INTEGER NOT NULL, " +
                    "dismissedAt INTEGER NOT NULL, " +
                    "dismissAttempts INTEGER NOT NULL, " +
                    "snoozed INTEGER NOT NULL, " +
                    "typedText TEXT NOT NULL)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habitos ADD COLUMN showInWidget INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN customHabitTypes TEXT NOT NULL DEFAULT 'Personalizado'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN mandatoryRemindersEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN mandatoryDeadlineTime TEXT NOT NULL DEFAULT '22:00'")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN mandatoryIntervalMinutes INTEGER NOT NULL DEFAULT 15")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN mandatoryAlarmSound INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN pinHash TEXT")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN pinSalt TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN hasSeededInitialData INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rotinaup.db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
