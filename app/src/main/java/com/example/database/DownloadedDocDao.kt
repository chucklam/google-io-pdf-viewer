/**
 * DownloadedDocDao.kt - Room Data Access Object for local sandboxed document records.
 *
 * This feature abstracts SQLite operations for local file indexes.
 * Use cases:
 * - Load dynamic flow list of all offline document files index.
 * - Retrieve details about specific downloaded documents.
 * - Add database receipts immediately after background stream files are completed.
 * - Purge documents from indexing.
 */
package com.example.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing the `downloaded_docs` table.
 */
@Dao
interface DownloadedDocDao {

    /**
     * Retrieve all downloaded files metadata sorted by completion date descending.
     *
     * @return Flow stream of a List of DownloadedDoc records.
     */
    @Query("SELECT * FROM downloaded_docs ORDER BY downloadTimestamp DESC")
    fun getAllDocuments(): Flow<List<DownloadedDoc>>

    /**
     * Retrieve metadata for a specific downloaded document using its identifier.
     *
     * @param id The unique identifier of the file in Room records.
     * @return The DownloadedDoc database model, or null if missing.
     */
    @Query("SELECT * FROM downloaded_docs WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DownloadedDoc?

    /**
     * Register a newly completed file download record in database index.
     *
     * @param doc The DownloadedDoc metadata record.
     * @return The auto-generated row primary key.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DownloadedDoc): Long

    /**
     * Delete an existing record of downloaded file from the SQLite index.
     * Note: This only deletes it from the database record; file system deletions are separate.
     *
     * @param doc The DownloadedDoc entity to remove.
     */
    @Delete
    suspend fun deleteDocument(doc: DownloadedDoc)

    /**
     * Delete metadata record for a document directly via its primary ID.
     *
     * @param id The unique database primary key ID.
     */
    @Query("DELETE FROM downloaded_docs WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)
}
