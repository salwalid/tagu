package com.tagu.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM saved_tags ORDER BY nickname ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM saved_tags WHERE id = :id")
    suspend fun getTagById(id: String): TagEntity?

    @Query("SELECT * FROM saved_tags WHERE lastPrivacyId = :privacyId")
    suspend fun getTagByPrivacyId(privacyId: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("UPDATE saved_tags SET lastPrivacyId = :privacyId, lastDeviceAddress = :address, lastSeenTimestamp = :timestamp, lastRssi = :rssi WHERE id = :id")
    suspend fun updateTagScan(id: String, privacyId: String, address: String, timestamp: Long, rssi: Int)

    @Query("SELECT * FROM saved_tags WHERE isMonitoring = 1")
    suspend fun getMonitoredTags(): List<TagEntity>
}
