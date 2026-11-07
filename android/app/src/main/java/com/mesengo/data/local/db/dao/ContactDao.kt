package com.mesengo.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mesengo.data.local.db.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY displayName ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE status = :status ORDER BY displayName ASC")
    fun getContactsByStatus(status: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE status = 'pending_received' ORDER BY displayName ASC")
    fun getIncomingRequests(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE status = 'pending_sent' ORDER BY displayName ASC")
    fun getOutgoingRequests(): Flow<List<ContactEntity>>

    @Query("UPDATE contacts SET status = 'pending_sent' WHERE status = 'pending'")
    suspend fun migrateLegacyPendingStatus()

    @Query("SELECT * FROM contacts WHERE contactUserId = :userId LIMIT 1")
    suspend fun getContactById(userId: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("UPDATE contacts SET status = :status WHERE contactUserId = :contactUserId")
    suspend fun updateContactStatus(contactUserId: String, status: String)

    @Query("DELETE FROM contacts WHERE contactUserId = :contactUserId")
    suspend fun deleteContact(contactUserId: String)
}
