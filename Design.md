# Design Document - PDF Web Browser App

This document outlines the architectural patterns, feature set, guidelines, and technical details for the **PDF Web Browser** Android application.

## Overview

The PDF Web Browser is a lightweight, edge-to-edge Android application featuring a built-in web browser, favorite websites bookmark management, automatic intercept-and-download functionality for PDF documents, and a fully functional offline document viewer.

---

## Key Features

1. **Built-in Web Browser**:
   - Integrated `WebView` featuring custom navigation controls (back, forward, refresh, home).
   - Dynamic Address Bar showing the current URL, with support for searching (via search engine) and direct domain navigation.
   - Live loading progress bar.

2. **Favorite Websites**:
   - Persisted using **Room Database**.
   - Shown as clear, actionable tactile cards for quick navigation.
   - Easy "Add Current Page" bookmark creator and delete gestures/options.

3. **Smart PDF Interception & Downloader**:
   - Background downloads handled asynchronously using modern `OkHttpClient`.
   - Automated interception inside WebView client for URLs ending with `.pdf`.
   - Broad standard download listener implementation to capture server-initiated octet-stream PDF downloads.
   - Download status indicator, saving to the internal app sandbox of local files directory (`context.filesDir`).

4. **100% Offline PDF Viewer**:
   - Render PDF pages directly within the Jetpack Compose layer using the native Android `android.graphics.pdf.PdfRenderer` API.
   - Zero-dependency offline viewing scheme: loads files directly from internal storage.
   - Smooth lazy loading of pages as high-fidelity `Bitmap` models, fully accessible without any internet or external dependencies.
   - **Automatic Action Trigger**: Success triggers on any PDF file download immediately open the file in the full-screen reader view for zero-friction immediate viewing.
   - **Bidirectional Zoom & Scroll Controls**: Leverages an expandable layout system. When the user adjusts zoom levels (via the action bar buttons), the entire page width expands visually, allowing seamless left-to-right panning combined with high-fidelity vertical scrolling.

---

## Architecture & Data Flow

We follow clean **MVVM architecture** with structured layers:

### 1. Data Layer (Room Persistence)
- **`FavoriteSite` Entity**: Holds favorited website title, URL, custom category/color, and created timestamp.
- **`DownloadedDoc` Entity**: Holds downloaded file name, local sandbox file path, online source URL, file size in bytes, and download timestamp.
- **`AppDatabase` & DAOs**: Reactively exposes bookmarks and downloads as `Flow<List<T>>`.

### 2. Service & Download Layer (`AppRepository`)
- Wraps DB access.
- Conducts asynchronous network downloads using a standard scoped OkHttpClient.
- Translates content streams directly to safe internal files, and logs operations.

### 3. Central Configuration (`AppConfig.kt`)
- Groups all key system constants, default bookmarks, browser search engines, etc., in a single location for maintainability.

### 4. Presentation Layer (`BrowserViewModel` & Composable UI)
- The app implements the **Sleek Interface** design theme, which focuses on sophisticated, warm neutral tones, rounded structural boundaries, and custom lavenders:
  - **Sleek Warm Theme (Material 3)**: Tailors active light color schemes to `#fdf8f6` (canvas backdrop), with containers in `#f3eef4` and borders/outline accents in `#eaddff`.
  - **Custom Tab Selection Pills**: Leverages transparent layout indices with a prominent `#e8def8` rounded navigation shape representing selected menu options.
  - **Browser Frame**: Features an address bar integrated into a custom fully rounded capsule mimicking public browser address lines, styled in `#f3eef4` and `#eaddff`.
  - **Offline Library Frame**: Lists all saved documents as stylized tactile cards (color `#f3eef4` with border `#eaddff`) styled identically to custom system storage elements.
  - **Theme-aligned PDF Reader segment**: Features specialized Top Bar styling with uppercase labels ("PDF VIEWER" in `#6750a4`) and a `#eaddff` border separator mimicking the spec.

5. **Gemini PDF-to-Markdown Extraction & Reader Pipeline**:
   - **Automated AI Extraction**: Integrates `GeminiExtractor` which feeds raw PDF document bytes (Base64) alongside layouts questions directly to `gemini-3.5-flash` to parse high-fidelity structured Markdown texts.
   - **Unified Sandbox Storage**: Stores parsed `.md` companion files on disk directly beside the corresponding source `.pdf` files.
   - **Coordinated Storage Lifecycle**: Deleting a PDF item in the library safely cleans up and deletes both the `.pdf` and the companion `.md` files concurrently.
   - **High-Timeout Resiliency & Fallback**: Configures 60s network read/write thresholds on OkHttp. Dispatches custom, detailed layout index indicators (using native `PdfRenderer` structures) on off-grid run sequences or missing API credentials to guarantee uninterrupted usage.
   - **In-App Text Reader View**: Features the `OfflineMarkdownViewer` which renders custom Material 3 bullet points, quote callouts, headers, and metadata, together with rapid copy-to-clipboard buttons.

---

## Technical Specifications & Logging

- **SDK Targets & Versions**: Android 16 (API 36) SDK, compiled with Jetpack Compose.
- **Docstrings & Logging**: Every class and function preserves rigorous docstrings describing its scope. All operations publish clear logging containing execution parameters.
  - **Generative AI Logging Protocol**: Automatically tracks all GenAI parameters (e.g. model, active prompt, file length coordinates) and captures resultant response previews as diagnostic indicators, stripping out verbose raw Base64 contents to keep log indices readable.
- **State Preservation**: Retains ViewModel activity flow reactively, safeguarding standard edge-to-edge rendering insets.

---

## Testing Verification

The layout and DB components are tested through:
1. Direct compiler execution to verify layout bounds.
2. Unit tests targeting bookmarks/downloads metadata.
3. Test utilities within `AppConfig` allowing users to inject sample favorites and fake PDF mock triggers without corrupting storage files.
4. **Resiliency Validation**: Automated fallbacks guarantee that when the network drops or API keys are unprovided, offline outline sheets are compiled dynamically for direct verification inside the UI.
