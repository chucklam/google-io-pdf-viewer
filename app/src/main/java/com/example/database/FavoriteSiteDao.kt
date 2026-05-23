/**
 * FavoriteSiteDao.kt - Room Data Access Object for bookmarked favorite websites.
 *
 * This feature abstracts the SQLite queries needed to save, delete, and list
 * favorite websites.
 * Use cases:
 * - Load dynamic flow list of favorite bookmarks for reactivity in Compose.
 * - Insert or replace websites into database records on user bookmark selection.
 * - Delete target favorites securely.
 */
package com.example.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing the `favorite_sites` table.
 */
@Dao
interface FavoriteSiteDao {

    /**
     * Retrieve all saved favorite websites sorted by creation timestamp descending.
     * Reactively streams list notifications to the front-layer.
     *
     * @return Flow stream of a List of FavoriteSite records.
     */
    @Query("SELECT * FROM favorite_sites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteSite>>

    /**
     * Insert a new website bookmark. Overwrites matching unique fields in conflict.
     *
     * @param site The FavoriteSite entity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(site: FavoriteSite): Long

    /**
     * Delete an existing favorite bookmark record from the database.
     *
     * @param site The FavoriteSite entity to remove.
     */
    @Delete
    suspend fun deleteFavorite(site: FavoriteSite)

    /**
     * Delete a favorite bookmark from the database via its unique system ID.
     *
     * @param id The unique database primary key ID.
     */
    @Query("DELETE FROM favorite_sites WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)
}
