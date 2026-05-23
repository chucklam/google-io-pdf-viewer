/**
 * AppDatabase.kt - Core Room SQLite Database instantiation.
 *
 * This component brings together our database schemas, entities, and DAOs, forming
 * the single absolute SQLite interface for our application storage.
 * Use cases:
 * - Initialize SQLite configuration on application startup.
 * - Build and serve single-point Database instance access across operations.
 * - Register callbacks/setup steps for first launch data insertion (seed mock websites).
 */
package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.AppConfig

/**
 * Android Room SQLite Database holder.
 * Indexes tables representing favorite web links and downloaded offline resources.
 */
@Database(entities = [FavoriteSite::class, DownloadedDoc::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Obtains the DAO interface for managing bookmarked favorite sites.
     */
    abstract fun favoriteSiteDao(): FavoriteSiteDao

    /**
     * Obtains the DAO interface for indexing downloaded system documents.
     */
    abstract fun downloadedDocDao(): DownloadedDocDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Accesses or initializes the synchronized thread-safe Singleton database.
         *
         * @param context Host application context.
         * @return The active AppDatabase instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            AppConfig.logCall(AppConfig.LOG_TAG_DATABASE, "getDatabase", "context" to context.packageName)
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pdf_browser_database"
                )
                // Fallback to destructive migration to simplify layout revisions
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
