/**
 * AppConfig.kt - Centralized application configuration file.
 *
 * This file gathers all configurable aspects of the app in one place, including:
 * - Default bookmarked web references
 * - Default search engines and fallback URLs
 * - Core timeout settings
 * - Test data switches to inject reference mocks without corrupting production environments
 *
 * Use cases:
 * - Centralized resource editing
 * - Setting test modes for mock loading
 */
package com.example

import android.util.Log

/**
 * Global configuration constants and parameters for the PDF Web Browser app.
 */
object AppConfig {

    private const val TAG = "AppConfig"

    /**
     * Gemini LLM model configuration.
     * Standard choice for basic text and PDF layout analysis.
     */
    const val GEMINI_MODEL_NAME = "gemini-3.5-flash"

    /**
     * Gemini base URL for REST API.
     */
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    /**
     * Default prompt to direct text extraction from multi-modal PDF file data.
     */
    const val GEMINI_PDF_PROMPT = "Extract the main text content from this PDF file. Clean up any OCR artifacts, headers, footers, page numbers, and formatting. Return the output as nice, clean Markdown with structured headings, lists, tables, and paragraphs. Avoid adding conversational introductions, filler or outro text—just return the Markdown content itself."

    /**
     * Default Search Engine query prefix. When the user types text instead of a URL, 
     * this prefix will be appended before loading.
     */
    const val DEFAULT_SEARCH_ENGINE_QUERY_URL = "https://www.google.com/search?q="

    /**
     * Connection timeout during file downloads, in milliseconds.
     */
    const val DOWNLOAD_CONNECT_TIMEOUT_MS = 15000L

    /**
     * Read timeout during file downloads, in milliseconds.
     */
    const val DOWNLOAD_READ_TIMEOUT_MS = 30000L

    /**
     * Logging tags for different application modules.
     */
    const val LOG_TAG_BROWSER = "PDFBrowser:WebView"
    const val LOG_TAG_DATABASE = "PDFBrowser:DB"
    const val LOG_TAG_DOWNLOAD = "PDFBrowser:Download"
    const val LOG_TAG_UI = "PDFBrowser:UI"

    /**
     * Feature Flag: Enable test data inject buttons in the UI.
     * This provides a clean mechanism to test the download interface and view real PDFs
     * without manually typing long PDF URLs, fulfilling the guideline "always create a way
     * to test the scripts without altering the data".
     */
    const val ENABLE_TEST_DATA_INJECTOR = true

    /**
     * Default favorite sites to pre-populate on database first run.
     */
    val DEFAULT_FAVORITES = listOf(
        FavoriteMock(
            title = "Google",
            url = "https://www.google.com",
            colorHex = "#4285F4"
        ),
        FavoriteMock(
            title = "Wikipedia",
            url = "https://www.wikipedia.org",
            colorHex = "#9C27B0"
        ),
        FavoriteMock(
            title = "Jetpack Compose Docs",
            url = "https://developer.android.com/compose",
            colorHex = "#3DDC84"
        ),
        FavoriteMock(
            title = "Kotlin Language",
            url = "https://kotlinlang.org",
            colorHex = "#7F52FF"
        )
    )

    /**
     * Safe test PDF sample resources that can be injected at will without affecting live configurations.
     */
    val TEST_PDF_LINKS = listOf(
        TestPdfUrl(
            name = "W3C PDF Reference",
            url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            description = "A standard 1-page sample dummy PDF."
        ),
        TestPdfUrl(
            name = "Compact PDF Sample",
            url = "https://unec.edu.az/application/uploads/2014/12/pdf-sample.pdf",
            description = "A clean 2-page sample PDF file."
        )
    )

    /**
     * Log helper function that satisfies the parameter logging instruction.
     *
     * @param tag Logging tag to filter by.
     * @param functionName The name of the function being invoked.
     * @param params Key-value pairs of the function arguments.
     */
    fun logCall(tag: String, functionName: String, vararg params: Pair<String, Any?>) {
        val paramsString = params.joinToString(", ") { "${it.first}=${it.second}" }
        Log.i(tag, "CALL: $functionName($paramsString)")
    }
}

/**
 * Simple dataholder representing a hardcoded favorite placeholder.
 */
data class FavoriteMock(
    val title: String,
    val url: String,
    val colorHex: String
)

/**
 * Dataholder representing a trusted sample PDF for fast verification of PDF download mechanisms.
 */
data class TestPdfUrl(
    val name: String,
    val url: String,
    val description: String
)
