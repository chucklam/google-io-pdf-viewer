/**
 * GeminiExtractor.kt - PDF to Markdown text parsing service using Gemini AI API.
 *
 * This feature drives multi-modal text extraction on local PDF files. It converts binary files 
 * to Base64 inline-data blocks, encapsulates them inside native Gemini API structures, and executes 
 * POST commands to the central Generative Language endpoint for deep text parsing.
 *
 * Use cases:
 * - Read local PDF files into bytes, securing safe Base64 representation.
 * - Conduct high-timeout POST calls using OkHttp to avoid premature read failures.
 * - Parse JSON streams cleanly leveraging Moshi adapters.
 * - Log execution contexts and parameter packets (excluding excessive raw image/pdf stream data).
 */
package com.example.api

import android.util.Base64
import android.util.Log
import com.example.AppConfig
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

// --- Gemini REST API Request & Response Schema Models ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

/**
 * Service class performing generative media text parsing under high isolation standards.
 */
object GeminiExtractor {

    private const val TAG = "PDFBrowser:GeminiExtractor"

    // High timeout client to navigate large multi-modal uploads
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Extracts text content from a given local PDF file using Gemini AI capabilities.
     * Encodes raw PDF data into Base64 format and sends it to the Gemini API.
     *
     * @param pdfFile The system File reference targeting local PDF cache.
     * @return Fully formatted extracted Markdown text.
     */
    fun extractTextFromPdf(pdfFile: File): String {
        AppConfig.logCall(TAG, "extractTextFromPdf", "file" to pdfFile.name)

        // Retrieve API key dynamically from compiled environment variables
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val errorMsg = "Gemini API Key is blank or placeholder. Fallback required."
            Log.w(TAG, errorMsg)
            throw IllegalStateException(errorMsg)
        }

        // Convert target file content to safe Base64 string
        val fileBytes = FileInputStream(pdfFile).use { it.readBytes() }
        val base64Data = Base64.encodeToString(fileBytes, Base64.NO_WRAP)

        // Structure direct Request model
        val requestBodyModel = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = AppConfig.GEMINI_PDF_PROMPT),
                        GeminiPart(inlineData = GeminiInlineData(mimeType = "application/pdf", data = base64Data))
                    )
                )
            )
        )

        // Logs the generative AI invocation details in compliance with additional instructions (strip huge base64)
        Log.i(
            TAG,
            "GENAI_CALL: model=${AppConfig.GEMINI_MODEL_NAME}, prompt='${AppConfig.GEMINI_PDF_PROMPT}', data_length=${base64Data.length}"
        )

        // Serialize the JSON request payload
        val requestAdapter = moshi.adapter(GeminiRequest::class.java)
        val jsonPayload = requestAdapter.toJson(requestBodyModel)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        // Target direct model endpoint per REST guides
        val requestUrl = "${AppConfig.GEMINI_BASE_URL}v1beta/models/${AppConfig.GEMINI_MODEL_NAME}:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errText = "Gemini API Network Error: code=${response.code}, message=${response.message}"
                Log.e(TAG, errText)
                throw IllegalStateException(errText)
            }

            val responseBodyString = response.body?.string()
                ?: throw IllegalStateException("Empty response body returned by Gemini API")

            val responseAdapter = moshi.adapter(GeminiResponse::class.java)
            val parsedResponse = responseAdapter.fromJson(responseBodyString)

            val extractedText = parsedResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("No valid parsing candidate in Gemini API output stream.")

            // Log output of generative AI call in compliance with additional instructions
            Log.i(TAG, "GENAI_OUTPUT: output_length=${extractedText.length}, preview='${extractedText.take(150)}...'")
            return extractedText
        }
    }
}
