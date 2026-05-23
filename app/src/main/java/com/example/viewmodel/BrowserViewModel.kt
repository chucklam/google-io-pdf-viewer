/**
 * BrowserViewModel.kt - Live State Handler for Web Browsing and Offline Docs.
 *
 * This ViewModel bridges Room services, async OkHttp download jobs, and Jetpack Compose screens.
 * Use cases:
 * - Hold reactive search parameters and state flags (loading progress, back/forward capabilities).
 * - Monitor database states for favorite shortcuts and offline documents list.
 * - Trigger silent background download tasks on PDF link identification.
 * - Expose active offline PDF document holders for visual rendering triggers.
 * - Pre-populate standard mock bookmarks if empty to accelerate system dry-run testing.
 */
package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppConfig
import com.example.FavoriteMock
import com.example.database.AppRepository
import com.example.database.DownloadedDoc
import com.example.database.DownloadResult
import com.example.database.FavoriteSite
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * AndroidViewModel governing browser commands, tab layouts, and local filesystem documents.
 */
class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application.applicationContext)

    // Current page URL (WebView navigation trigger)
    private val _currentUrl = MutableStateFlow("https://www.google.com")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    // Real-time text in the display address input field
    private val _addressInput = MutableStateFlow("https://www.google.com")
    val addressInput: StateFlow<String> = _addressInput.asStateFlow()

    // Browser navigation capability indices
    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    // Loading indicator toggles
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    // Live list of bookmarks
    val favorites: StateFlow<List<FavoriteSite>> = repository.allFavorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Live list of offline files
    val downloadedDocuments: StateFlow<List<DownloadedDoc>> = repository.allDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Track state of currently downloading file list
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    // Single overlay target for offline reading
    private val _activeOfflinePdf = MutableStateFlow<DownloadedDoc?>(null)
    val activeOfflinePdf: StateFlow<DownloadedDoc?> = _activeOfflinePdf.asStateFlow()

    // Single overlay target for offline Markdown reading
    private val _activeOfflineMarkdown = MutableStateFlow<DownloadedDoc?>(null)
    val activeOfflineMarkdown: StateFlow<DownloadedDoc?> = _activeOfflineMarkdown.asStateFlow()

    // Toast/Snackbar notifications channel
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Active visual tab selector: 0 for Browser, 1 for Saved Docs
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "BrowserViewModel:init")
        seedInitialFavoritesIfEmpty()
    }

    /**
     * Set the selected tab index (0=Browser, 1=Offline PDF Library).
     */
    fun selectTab(index: Int) {
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "selectTab", "index" to index)
        _selectedTab.value = index
    }

    /**
     * Seeds initial web links if bookmarks table is empty, preventing dry-runs.
     */
    private fun seedInitialFavoritesIfEmpty() {
        viewModelScope.launch {
            try {
                // Safely fetch only the first emission to inspect if seeded favorites exist
                val list = repository.allFavorites.first()
                if (list.isEmpty()) {
                    Log.i(AppConfig.LOG_TAG_DATABASE, "No bookmarks indexed. Prepopulating default references.")
                    AppConfig.DEFAULT_FAVORITES.forEach { bookmark ->
                        repository.insertFavorite(
                            FavoriteSite(
                                title = bookmark.title,
                                url = bookmark.url,
                                colorHex = bookmark.colorHex
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(AppConfig.LOG_TAG_DATABASE, "Failed to seed default favorites on startup", e)
            }
        }
    }

    /**
     * Fires on user address input confirmation. Converts raw text to Web URLs or Search Queries.
     *
     * @param input Raw text typed into the address bar.
     */
    fun processAddressSubmit(input: String) {
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "processAddressSubmit", "input" to input)
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val targetUrl = when {
            // Direct domain / web URL
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                trimmed
            }
            // Smart layout for domains like google.com, w3.org
            trimmed.contains(".") && !trimmed.contains(" ") -> {
                "https://$trimmed"
            }
            // Treat as query for engine
            else -> {
                AppConfig.DEFAULT_SEARCH_ENGINE_QUERY_URL + trimmed.replace(" ", "+")
            }
        }

        _currentUrl.value = targetUrl
        _addressInput.value = targetUrl
        // Always route focus to the browser window when navigating
        _selectedTab.value = 0
    }

    /**
     * Set current address text field value (for syncing keyboard typing).
     */
    fun updateAddressInput(input: String) {
        _addressInput.value = input
    }

    /**
     * Inform the ViewModel of incoming WebView page transitions.
     */
    fun onUrlLoadedInBrowser(url: String) {
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "onUrlLoadedInBrowser", "url" to url)
        _currentUrl.value = url
        _addressInput.value = url
    }

    /**
     * Update navigation state flags.
     */
    fun updateNavigationState(canBack: Boolean, canForward: Boolean) {
        _canGoBack.value = canBack
        _canGoForward.value = canForward
    }

    /**
     * Toggle browser screen network/page loading progress.
     */
    fun updateLoadingState(loading: Boolean, progress: Int = 0) {
        _isLoading.value = loading
        _loadingProgress.value = progress
    }

    /**
     * Pin the currently active webpage to the dynamic Favorites Room Database index.
     *
     * @param title Title of the website.
     * @param url The exact website URL.
     */
    fun addCurrentPageToFavorites(title: String, url: String) {
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "addCurrentPageToFavorites", "title" to title, "url" to url)
        viewModelScope.launch {
            if (url.isBlank()) {
                _toastMessage.emit("Cannot bookmark an empty address")
                return@launch
            }
            val label = title.trim().ifEmpty { "Web Bookmark" }
            val colorOptions = listOf("#4285F4", "#34A853", "#FBBC05", "#EA4335", "#9C27B0", "#00BCD4")
            val selectedColor = colorOptions[(label.length + url.length) % colorOptions.size]

            val fav = FavoriteSite(
                title = label,
                url = url,
                colorHex = selectedColor
            )
            repository.insertFavorite(fav)
            _toastMessage.emit("Added to Favorites: $label")
        }
    }

    /**
     * Remove a website bookmark from database registry.
     */
    fun removeFavoriteSite(id: Long) {
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "removeFavoriteSite", "id" to id)
        viewModelScope.launch {
            repository.deleteFavoriteById(id)
            _toastMessage.emit("Bookmark removed")
        }
    }

    /**
     * Queues asynchronous PDF resource fetching.
     *
     * @param url Link targeting a remote PDF asset.
     */
    fun startPdfFileDownload(url: String, contentDisposition: String? = null, mimeType: String? = null) {
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "startPdfFileDownload", "url" to url)
        viewModelScope.launch {
            _isDownloading.value = true
            _toastMessage.emit("Starting download...")

            val result = repository.downloadAndRecordFile(url, contentDisposition, mimeType)

            _isDownloading.value = false
            when (result) {
                is DownloadResult.Success -> {
                    _toastMessage.emit("Download complete: ${result.doc.fileName}")
                    // Automatically prompt user to switch to offline list to see downloaded PDF
                    _selectedTab.value = 1
                    // Automatically open the downloaded PDF in the interactive in-app viewer
                    _activeOfflinePdf.value = result.doc
                }
                is DownloadResult.Failure -> {
                    _toastMessage.emit("Download failed: ${result.errorMessage}")
                }
            }
        }
    }

    /**
     * Permanently deletes a saved offline file from both Room records and device storage.
     */
    fun deleteOfflineDocument(doc: DownloadedDoc) {
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "deleteOfflineDocument", "id" to doc.id)
        viewModelScope.launch {
            val deleted = repository.deleteDocument(doc)
            if (deleted) {
                _toastMessage.emit("Removed offline file: ${doc.fileName}")
                // If the active viewed pdf was deleted, close it
                if (_activeOfflinePdf.value?.id == doc.id) {
                    _activeOfflinePdf.value = null
                }
                // If the active viewed markdown was deleted, close it
                if (_activeOfflineMarkdown.value?.id == doc.id) {
                    _activeOfflineMarkdown.value = null
                }
            } else {
                _toastMessage.emit("Could not complete file purge.")
            }
        }
    }

    /**
     * Opens or closes an offline document layout for on-device PDF reading.
     *
     * @param doc The document meta targeting local file, or null to close.
     */
    fun setActiveOfflineDocForReading(doc: DownloadedDoc?) {
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "setActiveOfflineDocForReading", "docId" to doc?.id)
        _activeOfflinePdf.value = doc
    }

    /**
     * Opens or closes an offline document layout for in-app Markdown reading.
     *
     * @param doc The document meta targeting local file, or null to close.
     */
    fun setActiveOfflineMarkdownForReading(doc: DownloadedDoc?) {
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "setActiveOfflineMarkdownForReading", "docId" to doc?.id)
        _activeOfflineMarkdown.value = doc
    }

    /**
     * Injects custom test URLs instantly inside system bookmarks and offline list.
     * Fulfills "always provide a way to test scripts/capabilities without messing with master data."
     */
    fun injectSystemTestData() {
        AppConfig.logCall(AppConfig.LOG_TAG_UI, "injectSystemTestData")
        viewModelScope.launch {
            // Seed a direct quick favorite for standard verification URL
            val sampleFav = FavoriteSite(
                title = "W3C Sample Files",
                url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/",
                colorHex = "#FF5722"
            )
            repository.insertFavorite(sampleFav)
            _toastMessage.emit("Injected test favorite card (W3C Sample)")
        }
    }
}
