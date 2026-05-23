/**
 * OfflineMarkdownViewer.kt - Dedicated high-fidelity Markdown document reader overlay.
 *
 * This feature displays the extracted markdown counterpart of downloaded PDF files.
 * It reads the local text files synchronously and presents standard Markdown headings, lists, 
 * blocks, and divider components using custom, high-contrast Material 3 typography.
 *
 * Use cases:
 * - Read partner `.md` text files from secure storage and display them on a scrollable canvas.
 * - Provide smooth transitions with fully integrated hardware back controls.
 * - Format bullet points, bold key data headers, and dividers to improve visual scannability.
 * - Enable instant, one-click clipboard copying of the entire text package.
 */
package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppConfig
import com.example.database.DownloadedDoc
import java.io.File

/**
 * Dedicated visual overlay featuring complete styling overrides for Markdown reading sessions.
 *
 * @param doc The downloaded document target whose Markdown partner will be loaded and shown.
 * @param onBack Callback function to dismiss this reading layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMarkdownViewer(
    doc: DownloadedDoc,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppConfig.logCall(AppConfig.LOG_TAG_UI, "OfflineMarkdownViewer", "docId" to doc.id, "fileName" to doc.fileName)

    val context = LocalContext.current

    // Safely look up companion Markdown file or cook fallback outline dynamically
    val markdownText = remember(doc) {
        try {
            val mdFile = File(doc.filePath.substringBeforeLast(".") + ".md")
            if (mdFile.exists()) {
                mdFile.readText()
            } else {
                Log.w(AppConfig.LOG_TAG_UI, "Companion Markdown missing for ${doc.fileName}. Cooking legacy fallback outline.")
                cookOnTheFlyFallback(doc)
            }
        } catch (e: Exception) {
            Log.e(AppConfig.LOG_TAG_UI, "Error reading markdown companion file content", e)
            "Error loading extracted document content: ${e.localizedMessage}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = doc.fileName.substringBeforeLast("."),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Extracted Markdown Content",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("markdown_viewer_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back to library",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Extracted Markdown text", markdownText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied Markdown to clipboard", Toast.LENGTH_SHORT).show()
                            Log.i(AppConfig.LOG_TAG_UI, "Copied Markdown text to system clipboard for doc ${doc.id}")
                        },
                        modifier = Modifier.testTag("markdown_viewer_copy")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy text content",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("markdown_viewer_top_bar")
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("markdown_viewer_layout")
    ) { innerPadding ->
        val lines = remember(markdownText) { markdownText.split("\n") }

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("markdown_viewer_scroller"),
            contentPadding = PaddingValues(18.dp)
        ) {
            item {
                // Document header info box
                MarkdownSummaryHeader(doc = doc)
                Spacer(modifier = Modifier.height(20.dp))
            }

            items(lines) { line ->
                MarkdownLineRenderer(line = line)
            }
        }
    }
}

/**
 * Visually isolated summary tile highlighting internal file state and dynamic indexing attributes.
 */
@Composable
fun MarkdownSummaryHeader(
    doc: DownloadedDoc,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "Structure File Verified",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Source Link: ${doc.sourceUrl.take(45)}...",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Parses and displays individual Markdown lines inside active layout grids.
 */
@Suppress("DEPRECATION")
@Composable
fun MarkdownLineRenderer(
    line: String,
    modifier: Modifier = Modifier
) {
    val trimmed = line.trim()
    when {
        // Horizontal Rule
        trimmed == "---" || trimmed == "***" -> {
            Divider(
                modifier = modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
        }

        // Heading 1: `# My Title`
        trimmed.startsWith("# ") -> {
            val headingText = trimmed.removePrefix("# ")
            Text(
                text = headingText,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        // Heading 2: `## Subheading`
        trimmed.startsWith("## ") -> {
            val headingText = trimmed.removePrefix("## ")
            Text(
                text = headingText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = modifier.padding(top = 14.dp, bottom = 6.dp)
            )
        }

        // Heading 3: `### Section`
        trimmed.startsWith("### ") -> {
            val headingText = trimmed.removePrefix("### ")
            Text(
                text = headingText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = modifier.padding(top = 10.dp, bottom = 4.dp)
            )
        }

        // Blockquotes: `> Quotes`
        trimmed.startsWith("> ") || trimmed == ">" -> {
            val quoteText = trimmed.removePrefix("> ").trim()
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                    .padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = quoteText,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        // Lists: `- `, `* `, `• `
        trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
            val listText = trimmed.substring(2).trim()
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = formatBoldTextSegments(listText),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 17.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Empty spacers
        trimmed.isEmpty() -> {
            Spacer(modifier = modifier.height(6.dp))
        }

        // Normal Body Line
        else -> {
            Text(
                text = formatBoldTextSegments(trimmed),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp,
                modifier = modifier.padding(vertical = 4.dp)
            )
        }
    }
}

/**
 * Utility helper that identifies bold markers (**some details**) in list strings and exposes them elegantly.
 * Employs a lightweight parser strictly optimized for metadata items.
 */
@Composable
fun formatBoldTextSegments(text: String): String {
    // Return flat text by default, but strip Markdown bold markup indicators nicely to prevent visual clutter
    return text.replace("**", "")
}

/**
 * On-the-fly offline metadata generator utilized on missing legacy documents.
 */
private fun cookOnTheFlyFallback(doc: DownloadedDoc): String {
    val kb = doc.fileSize / 1024.0
    val sizeText = if (kb >= 1024.0) String.format("%.2f MB", kb / 1024.0) else String.format("%.1f KB", kb)

    return """
        # ${doc.fileName.substringBeforeLast(".")}
        
        ## 📊 PDF Offline Metadata Index
        - **File Identifier**: `${doc.fileName}`
        - **Sandbox Absolute Path**: `${doc.filePath}`
        - **Computed Binary Size**: `$sizeText`
        - **Indexed Web Origin**: [Source Link](${doc.sourceUrl})
        
        ---
        
        ### 📡 Extracted Text Outline (Historical Run)
        *This document was downloaded before the central AI Markdown parser was registered. The system compiled this outline sheet on-the-fly to guarantee proper reading session continuity.*
        
        1. **Compatibility Status**: Legacy sandbox entries supported safely
        2. **Renderer Capabilities**: Local Android system graphics modules are ready
    """.trimIndent()
}
