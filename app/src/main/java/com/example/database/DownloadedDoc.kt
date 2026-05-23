/**
 * DownloadedDoc.kt - Persisted data model for offline documents.
 *
 * This feature manages metadata for documents downloaded from online URLs to the local device 
 * sandbox folder. It allows indexing files inside local storage and rendering them cleanly offline.
 * Use cases:
 * - List all downloaded PDF files in the offline storage tab with size and time information.
 * - Retrieve local file paths to boot the offline internal PdfRenderer.
 * - Delete downloaded PDFs securely from both application storage and database index.
 */
package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a document downloaded locally for full offline viewing.
 * Metadata persisted in the local Room database index to avoid disk sweeps.
 *
 * @property id The auto-generated unique identifier for this document record.
 * @property fileName The friendly displaying name (usually extracted from the URL or headers).
 * @property filePath The absolute sandboxed local path on the Android file system.
 * @property fileSize Filesize measured in bytes.
 * @property downloadTimestamp Epoch time in milliseconds specifying when the download was finalized.
 * @property sourceUrl The web source link where this file was intercepted from.
 */
@Entity(tableName = "downloaded_docs")
data class DownloadedDoc(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val downloadTimestamp: Long = System.currentTimeMillis(),
    val sourceUrl: String
)
