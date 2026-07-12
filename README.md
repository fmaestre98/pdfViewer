# PdfViewer

A modern, visually immersive, and feature-rich Android PDF Reader application built with Jetpack Compose. Instead of a standard file list, it features a warm, realistic **Bookshelf Interface** to display your PDF library and comes packed with tools to annotate, highlight, bookmark, and read documents seamlessly.

---

##  Key Features

### 1. Interactive Bookshelf Library 
*   **Visual Shelf Layout**: Books are displayed as colorful covers placed on a wooden shelf row.
*   **Unique Customizations**: Covers have randomized colors and subtle tilt rotations to mimic a physical library.
*   **Storage Access**: Easily import PDFs from local storage.

### 2. Custom Interactive PDF Viewer 
*   **Smooth Gesture Controls**: Support for pinch-to-zoom, pan, and double-tap zoom for readability.
*   **Auto-Resume**: Remembers your exact position and resumes from the last read page for each document automatically.
*   **Edge-to-Edge Experience**: Utilizes modern Android inset handling for immersive reading.

### 3. Annotation & Study Tools 
*   **Bookmarks**: Bookmark critical pages and access them via a quick toggle.
*   **Sticky Page Notes**: Save, edit, and delete text notes attached to any specific page in the document.
*   **Text Highlighting**: Highlight text snippets in various colors (coordinates and text snippets are stored in the local database).

### 4. Visual Thumbnail Drawer 
*   **Thumbnail Previews**: Open a sliding drawer to view page thumbnails, enabling rapid visual navigation through the document.

---

## Technology Stack

The application leverages the latest modern Android development tools and best practices:

*   **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) (100% Declarative UI)
*   **Dependency Injection**: [Dagger Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
*   **Local Database**: [Room SQL Database](https://developer.android.com/training/data-storage/room) (Handles book metadata, bookmarks, highlights, and page notes)
*   **PDF Rendering Engine**: [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) (For robust PDF parsing, text selection, and rendering)
*   **Navigation**: [Jetpack Compose Navigation](https://developer.android.com/guide/navigation/navigation-principles)
*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **Build System**: Gradle Kotlin DSL with Version Catalogs (`libs.versions.toml`)

---

##  Architecture

The app is built following **Clean Architecture** guidelines and the **MVI (Model-View-Intent)** presentation pattern:

```
com.fmaestre98.pdfviewer
│
├── di/                   # Dependency Injection modules (Hilt)
├── models/               # Core Domain/Business Models (e.g., Book)
├── pdfViewer/            # Custom rendering, gestures, and PDF-specific helpers
├── repository/           # Repository interfaces and implementations
├── room/                 # Room database configuration, entities, and DAOs
│
└── ui/
    ├── navigation/       # Navigation routes and controllers
    ├── theme/            # Theme, color schemes, and typography
    └── screens/          # Screen-level composables
        ├── home/         # Home bookshelf screen (State, Action, Event, VM, Screen)
        └── reader/       # PDF Reader & Annotator screen (State, Action, Event, VM, Screen)
```

### Data Flow (MVI)
1.  **UI/Screens** emit **Actions** (e.g., `OnPageChanged`, `SavePageNote`) to the **ViewModel**.
2.  The **ViewModel** processes the action, interacts with **Repositories** (Room/PdfBox), and updates the **State**.
3.  The **UI** observes the **State** and recomposes reactively. One-time side effects (e.g., displaying Snackbars, navigation) are handled via **Events** channel.

---

##  Getting Started

### Prerequisites
*   Android Studio Ladybug (or newer)
*   JDK 17+
*   Android SDK 35+

### Build and Run
1.  Clone the repository:
    ```bash
    git clone https://github.com/fmaestre98/pdfViewer.git
    cd pdfViewer
    ```
2.  Open the project in Android Studio.
3.  Sync the project with Gradle files.
4.  Run the application on an emulator or a physical device (Android 8.0+ / API 26+ recommended).

---

##  License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Copyright (c) 2026 Fabian Ortiz
