package com.example.data.db

import androidx.room.*
import com.example.data.model.DailyReview
import com.example.data.model.NapLog
import com.example.data.model.SleepLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    // Night Sleep Logs
    @Query("SELECT * FROM sleep_logs WHERE date = :date LIMIT 1")
    fun getSleepLog(date: String): Flow<SleepLog?>

    @Query("SELECT * FROM sleep_logs ORDER BY date DESC")
    fun getAllSleepLogs(): Flow<List<SleepLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(log: SleepLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSleepLogs(logs: List<SleepLog>)

    @Query("DELETE FROM sleep_logs")
    suspend fun deleteAllSleepLogs()

    // Nap Logs (Separated)
    @Query("SELECT * FROM nap_logs WHERE date = :date LIMIT 1")
    fun getNapLog(date: String): Flow<NapLog?>

    @Query("SELECT * FROM nap_logs ORDER BY date DESC")
    fun getAllNapLogs(): Flow<List<NapLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNapLog(log: NapLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNapLogs(logs: List<NapLog>)

    @Query("DELETE FROM nap_logs")
    suspend fun deleteAllNapLogs()

    // Daily Reviews
    @Query("SELECT * FROM daily_reviews WHERE date = :date LIMIT 1")
    fun getDailyReview(date: String): Flow<DailyReview?>

    @Query("SELECT * FROM daily_reviews ORDER BY date DESC")
    fun getAllDailyReviews(): Flow<List<DailyReview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyReview(review: DailyReview): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDailyReviews(reviews: List<DailyReview>)

    @Query("DELETE FROM daily_reviews")
    suspend fun deleteAllDailyReviews()
}
