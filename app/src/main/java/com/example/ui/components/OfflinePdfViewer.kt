/**
 * OfflinePdfViewer.kt - Implements a fully-offline high-fidelity PDF Document Engine.
 *
 * This feature displays saved PDF files from local storage on-screen without requiring internet access.
 * Use cases:
 * - Load of any local PDF file from the device sandbox on selecting a file in Offline Library.
 * - Render pages lazily into Bitmaps in an isolated coroutine pool to maintain high framework FPS.
 * - Provide user navigations to return back or slide page lists.
 */
package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppConfig
import com.example.database.DownloadedDoc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fullscreen PDF Document Reading layout that operates totally offline using Android PdfRenderer.
 * Shows pages inside a lazy vertical column with responsive resizing.
 *
 * @param doc Metadata block representing the local device file to read.
 * @param onBack Callback fired when dismiss chevron or android back is triggered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflinePdfViewer(
    doc: DownloadedDoc,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppConfig.logCall(AppConfig.LOG_TAG_UI, "OfflinePdfViewer", "docId" to doc.id, "fileName" to doc.fileName)

    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var totalPages by remember { mutableStateOf(0) }
    var renderError by remember { mutableStateOf<String?>(null) }
    var scaleFactor by remember { mutableFloatStateOf(1.5f) } // default resolution scaling multiplier
    val pdfMutex = remember { Mutex() }
    val isClosed = remember { mutableStateOf(false) }

    // Lifecycle coordinator to construct and dispose PdfRenderer stream cleanly
    DisposableEffect(doc.filePath) {
        val file = File(doc.filePath)
        if (!file.exists()) {
            renderError = "Source document file does not exist locally anymore."
            Log.e(AppConfig.LOG_TAG_UI, "Offline file not found: ${doc.filePath}")
        } else {
            try {
                val parcelFD = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(parcelFD)
                fileDescriptor = parcelFD
                pdfRenderer = renderer
                totalPages = renderer.pageCount
                Log.i(AppConfig.LOG_TAG_UI, "Initialized PdfRenderer for path: ${doc.filePath}, totalPages: $totalPages")
            } catch (e: Exception) {
                renderError = "Could not parse file structure: ${e.localizedMessage}"
                Log.e(AppConfig.LOG_TAG_UI, "Fatal rendering error", e)
            }
        }

        // Clean up locks securely
        onDispose {
            isClosed.value = true
            val rendererToClose = pdfRenderer
            val fdToClose = fileDescriptor
            if (rendererToClose != null || fdToClose != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    pdfMutex.withLock {
                        try {
                            rendererToClose?.close()
                            fdToClose?.close()
                            Log.d(AppConfig.LOG_TAG_UI, "Successfully closed PdfRenderer stream leaks under lock.")
                        } catch (e: Exception) {
                            Log.e(AppConfig.LOG_TAG_UI, "Error releasing system file locks during dispose under lock", e)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "PDF VIEWER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF6750A4),
                                style = androidx.compose.ui.text.TextStyle(letterSpacing = 1.2.sp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = doc.fileName,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color(0xFF1D1B1E)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("pdf_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Return to Library",
                                tint = Color(0xFF49454F)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { if (scaleFactor > 1.0f) scaleFactor -= 0.25f }) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut, 
                                contentDescription = "Zoom Out",
                                tint = Color(0xFF49454F)
                            )
                        }
                        Text(
                            text = "${(scaleFactor * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B1E),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = { if (scaleFactor < 2.5f) scaleFactor += 0.25f }) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn, 
                                contentDescription = "Zoom In",
                                tint = Color(0xFF49454F)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF3EEF4),
                        titleContentColor = Color(0xFF1D1B1E)
                    )
                )
                // Subtle bottom border matching border-bottom border-[#eaddff] in Design HTML
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFEADDFF))
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f)) // Cinematic dark backing
        ) {
            val currentRenderer = pdfRenderer

            when {
                renderError != null -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Rendering Failure",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                renderError ?: "",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                currentRenderer == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Parsing structure offline...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                totalPages == 0 -> {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Empty document content", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                else -> {
                    val horizontalScrollState = rememberScrollState()
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val screenWidth = maxWidth
                        val pdfWidth = screenWidth * (scaleFactor / 1.5f).coerceAtLeast(1.0f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .width(pdfWidth)
                                    .fillMaxHeight()
                                    .padding(horizontal = 8.dp)
                                    .testTag("pdf_pages_list")
                            ) {
                                items(totalPages) { pageIndex ->
                                    PdfPageCard(
                                        renderer = currentRenderer,
                                        pageIndex = pageIndex,
                                        scaleFactor = scaleFactor,
                                        pdfMutex = pdfMutex,
                                        isClosed = { isClosed.value }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Isolated visual card component to display a single page Bitmap safely inside compose lazy list layouts.
 */
@Composable
fun PdfPageCard(
    renderer: PdfRenderer,
    pageIndex: Int,
    scaleFactor: Float,
    pdfMutex: Mutex,
    isClosed: () -> Boolean,
    modifier: Modifier = Modifier
) {
    var pageBitmap by remember(pageIndex, scaleFactor) { mutableStateOf<Bitmap?>(null) }
    var renderAspectRatio by remember { mutableFloatStateOf(0.707f) } // typical Letter layout default percentage
    var isRendering by remember(pageIndex, scaleFactor) { mutableStateOf(true) }

    LaunchedEffect(renderer, pageIndex, scaleFactor) {
        // Carry rendering execution in Background Coroutines
        withContext(Dispatchers.IO) {
            if (isClosed()) return@withContext
            pdfMutex.withLock {
                if (isClosed()) return@withContext
                try {
                    val page = renderer.openPage(pageIndex)
                    
                    // Establish accurate aspect sizes
                    val aspect = page.width.toFloat() / page.height.toFloat()
                    
                    // Multiply target sizes for sharp UI graphics
                    val baseTargetWidth = 800
                    val targetWidth = (baseTargetWidth * scaleFactor).toInt()
                    val targetHeight = (targetWidth / aspect).toInt()

                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    
                    // Canvas back-draw to clear transparency grids
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    withContext(Dispatchers.Main) {
                        renderAspectRatio = aspect
                        pageBitmap = bitmap
                        isRendering = false
                    }
                } catch (e: Exception) {
                    Log.e(AppConfig.LOG_TAG_UI, "Error rendering page index $pageIndex", e)
                    withContext(Dispatchers.Main) {
                        isRendering = false
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("pdf_page_$pageIndex"),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .aspectRatio(renderAspectRatio),
            contentAlignment = Alignment.Center
        ) {
            val activeBitmap = pageBitmap

            if (isRendering || activeBitmap == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Rendering page ${pageIndex + 1}...",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Image(
                    bitmap = activeBitmap.asImageBitmap(),
                    contentDescription = "PDF Page ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                )
                
                // Overlayed floating Page Label (bottom-right edge)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${pageIndex + 1}",
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
