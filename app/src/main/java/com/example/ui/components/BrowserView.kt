/**
 * BrowserView.kt - Built-in Multi-state Web Browser View for Jetpack Compose.
 *
 * This feature displays an embedded Android WebView capable of navigating to public URLs
 * and searching terms. It intercepts .pdf links during browsing and routes them to our
 * file downloader, keeping users inside the app's native flow.
 * Use cases:
 * - Enter web domains or terms into the address bar to browse materials.
 * - Display a neat dashboard of bookmarked Quick Cards on empty web buffers.
 * - Trigger background download routines automatically when remote PDF links are tapped.
 * - Provide back, forward, refresh, and add-to-favorites capabilities on tap.
 */
package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.DownloadListener
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.AppConfig
import com.example.database.FavoriteSite
import com.example.viewmodel.BrowserViewModel

/**
 * Built-in WebView frame supporting full URL typing, smart searches, quick favorite grids,
 * and background download interceptions.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserView(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    AppConfig.logCall(AppConfig.LOG_TAG_UI, "BrowserView")

    val currentUrl by viewModel.currentUrl.collectAsState()
    val addressInput by viewModel.addressInput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()
    val canGoBack by viewModel.canGoBack.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val favoritesList by viewModel.favorites.collectAsState()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    // Prevent infinite recomposition loops during asset load by tracking the last URL actually initiated or observed
    var lastLoadedUrl by remember { mutableStateOf("") }
    
    // Track whether the webview is in landing home screen mode (custom dashboard)
    var isHomeScreen by remember { mutableStateOf(true) }

    // Synchronize home screen trigger with url parameters
    LaunchedEffect(currentUrl) {
        isHomeScreen = (currentUrl == "about:blank" || currentUrl.isBlank())
    }

    Column(modifier = modifier.fillMaxSize()) {
        // TOP ADDRESS & SEARCH BAR (Sleek Interface customized theme)
        Surface(
            tonalElevation = 0.dp,
            color = Color(0xFFFDF8F6),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { viewModel.updateAddressInput(it) },
                        placeholder = { Text("Search or type web address...", color = Color(0xFF49454F).copy(alpha = 0.7f), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isHomeScreen) Icons.Default.Search else Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFF49454F)
                            )
                        },
                        trailingIcon = {
                            if (addressInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { 
                                        viewModel.updateAddressInput("") 
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear address bar",
                                        tint = Color(0xFF49454F)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                viewModel.processAddressSubmit(addressInput)
                                focusManager.clearFocus()
                            }
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B1E),
                            unfocusedTextColor = Color(0xFF1D1B1E),
                            focusedContainerColor = Color(0xFFF3EEF4),
                            unfocusedContainerColor = Color(0xFFF3EEF4),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFEADDFF)
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("browser_address_bar")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Dynamic Go/Bookmark current button
                    if (!isHomeScreen) {
                        val isBookmarked = favoritesList.any { it.url.equals(currentUrl, ignoreCase = true) }
                        IconButton(
                            onClick = {
                                if (isBookmarked) {
                                    favoritesList.find { it.url.equals(currentUrl, ignoreCase = true) }?.let {
                                        viewModel.removeFavoriteSite(it.id)
                                    }
                                } else {
                                    val pageTitle = webViewRef?.title ?: "Web Shortcut"
                                    viewModel.addCurrentPageToFavorites(pageTitle, currentUrl)
                                }
                            },
                            modifier = Modifier.testTag("browser_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Toggle bookmark for current page",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Action button to load test options when in dashboard to fulfill guidelines
                        IconButton(
                            onClick = {
                                viewModel.injectSystemTestData()
                            },
                            modifier = Modifier.testTag("inject_test_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Inject test favorites",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // LINEAR PROGRESS INDICATOR FOR PAGES LOADING
                AnimatedVisibility(
                    visible = isLoading && !isHomeScreen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { loadingProgress.toFloat() / 100f },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // BROWSER MAIN VIEWPORT
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isHomeScreen) {
                // BOOKMARKS LAUNCHER DASHBOARD
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Icon(
                        imageVector = Icons.Default.Web,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your Favorites Library",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap a favorite link below or enter a search query above.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (favoritesList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No bookmarks yet.\nLoading standard reference cards...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("favorites_grid")
                        ) {
                            items(favoritesList, key = { it.id }) { fav ->
                                FavoriteCard(
                                    favorite = fav,
                                    onClick = {
                                        viewModel.processAddressSubmit(fav.url)
                                    },
                                    onDelete = {
                                        viewModel.removeFavoriteSite(fav.id)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // EMBEDDED WEBVIEW
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                supportZoom()
                                builtInZoomControls = true
                                displayZoomControls = false
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    viewModel.updateLoadingState(true, 10)
                                    url?.let { 
                                        viewModel.onUrlLoadedInBrowser(it)
                                        lastLoadedUrl = it
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    viewModel.updateLoadingState(false, 100)
                                    url?.let { 
                                        viewModel.onUrlLoadedInBrowser(it)
                                        lastLoadedUrl = it
                                    }
                                    viewModel.updateNavigationState(
                                        canBack = view?.canGoBack() ?: false,
                                        canForward = view?.canGoForward() ?: false
                                    )
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val rUrl = request?.url?.toString() ?: return false
                                    if (rUrl.endsWith(".pdf", ignoreCase = true)) {
                                        // Intercept and download natively
                                        viewModel.startPdfFileDownload(rUrl)
                                        return true
                                    }
                                    return false
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    viewModel.updateLoadingState(newProgress < 100, newProgress)
                                }
                            }

                            // Capture standard PDF attachment clicks
                            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                viewModel.startPdfFileDownload(url, contentDisposition, mimetype)
                            })

                            loadUrl(currentUrl)
                            lastLoadedUrl = currentUrl
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        val sanitizedViewUrl = view.url?.removeSuffix("/") ?: ""
                        val sanitizedCurrentUrl = currentUrl.removeSuffix("/")
                        if (sanitizedViewUrl != sanitizedCurrentUrl && 
                            currentUrl.isNotEmpty() && 
                            currentUrl != "about:blank" && 
                            lastLoadedUrl != currentUrl
                        ) {
                            lastLoadedUrl = currentUrl
                            view.loadUrl(currentUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("custom_webview")
                )
            }
        }

        // FLOATING CONTROLLER NAVIGATION PANEL (Sleek Interface themed)
        Surface(
            tonalElevation = 0.dp,
            color = Color(0xFFF7F2F9),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Subtle top border divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFEADDFF))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBack && !isHomeScreen,
                        modifier = Modifier.testTag("browser_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate backward",
                            tint = if (canGoBack && !isHomeScreen) Color(0xFF6750A4) else Color(0xFF49454F).copy(alpha = 0.35f)
                        )
                    }

                    // Forward Button
                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForward && !isHomeScreen,
                        modifier = Modifier.testTag("browser_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Navigate forward",
                            tint = if (canGoForward && !isHomeScreen) Color(0xFF6750A4) else Color(0xFF49454F).copy(alpha = 0.35f)
                        )
                    }

                    // Refresh / Stop Button
                    IconButton(
                        onClick = { if (isLoading) webViewRef?.stopLoading() else webViewRef?.reload() },
                        enabled = !isHomeScreen,
                        modifier = Modifier.testTag("browser_refresh_button")
                    ) {
                        Icon(
                            imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (isLoading) "Stop page render" else "Refresh page content",
                            tint = if (!isHomeScreen) Color(0xFF6750A4) else Color(0xFF49454F).copy(alpha = 0.35f)
                        )
                    }

                    // Home Page reset trigger
                    IconButton(
                        onClick = { 
                            viewModel.processAddressSubmit("about:blank")
                            viewModel.updateAddressInput("")
                        },
                        modifier = Modifier.testTag("browser_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Launch landing dashboard",
                            tint = if (isHomeScreen) Color(0xFF6750A4) else Color(0xFF49454F)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Beautiful colored card supporting easy launch and deletes for bookmarks.
 */
@Composable
fun FavoriteCard(
    favorite: FavoriteSite,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contextColor = remember(favorite.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(favorite.colorHex))
        } catch (e: Exception) {
            Color(0xFF2196F3) // fallback royal blue
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("favorite_card_${favorite.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored dot representative representing the bookmark accent Color
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color = contextColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        favorite.title.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).testTag("delete_favorite_${favorite.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete bookmark shortcut",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = favorite.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = favorite.url.substringAfter("://").substringBefore("/"),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
