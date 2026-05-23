/**
 * FavoriteSite.kt - Persisted data model for bookmarked web addresses.
 *
 * This feature supports saving, retrieving, and displaying quick bookmarks to help the user
 * navigate easily inside the built-in browser.
 * Use cases:
 * - Load custom user bookmarked URLs on the home grid layout.
 * - Add a currently visited website tab to the favorite database with matching colors.
 * - Delete bookmarks that are no longer needed.
 */
package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user's bookmarked favorite website.
 * This entity is stored dynamically in the local Room database to allow
 * quick clicks and navigation.
 *
 * @property id The auto-generated unique identifier for this bookmark.
 * @property title The user-friendly display name of the webpage.
 * @property url The absolute web URL pointer.
 * @property colorHex Hexadecimal color representation used to paint the tile card.
 * @property timestamp Epoch time in milliseconds when the bookmark was created.
 */
@Entity(tableName = "favorite_sites")
data class FavoriteSite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val colorHex: String,
    val timestamp: Long = System.currentTimeMillis()
)
