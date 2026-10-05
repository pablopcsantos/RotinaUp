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
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitoDao
    abstract fun sleepDao(): SleepDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rotinaup.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
