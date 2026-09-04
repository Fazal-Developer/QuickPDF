# QuickPDF – Android Studio Project

## Overview
**QuickPDF** is a complete, offline PDF toolkit built with Java for Android.

**Package:** `com.quickpdf.app`  
**Min SDK:** 23 (Android 6.0)  
**Target SDK:** 34 (Android 14)

## Features
| Feature | Description |
|---|---|
| 📄 Merge PDF | Combine 2+ PDFs, drag-to-reorder |
| ✂️ Split PDF | Extract selected pages |
| 🗜️ Compress PDF | Low / Medium / High compression |
| 🖼️ Image → PDF | JPG/PNG to PDF, A4 pages |
| 👁️ PDF Viewer | Barteksc viewer, pinch-zoom, scroll |
| 🕐 Recent Files | Open, Share, Rename, Delete |
| ⚙️ Settings | Dark mode, Rate app, Share app |

## How to Open in Android Studio
1. Open **Android Studio** → `File → Open` → navigate to this folder
2. Wait for Gradle sync to complete (first sync downloads ~150MB)
3. Connect a device or start an emulator
4. Click **Run ▶**

## Libraries Used
| Library | Purpose |
|---|---|
| `com.tom-roush:pdfbox-android:2.0.27.0` | PDF merge, split, compress |
| `com.github.barteksc:android-pdf-viewer:3.2.0-beta.1` | PDF viewer |
| `androidx.room:room-runtime:2.6.1` | Recent files database |
| `com.github.bumptech.glide:glide:4.16.0` | Image thumbnails |
| `com.google.android.material:material:1.12.0` | Material Design UI |
| `androidx.navigation:navigation-fragment:2.7.7` | Navigation Component |

## Project Structure
```
app/src/main/java/com/quickpdf/app/
├── MainActivity.java           – Entry point, Navigation setup
├── QuickPdfApplication.java    – PdfBox initialization
├── ui/
│   ├── home/HomeFragment.java        – Dashboard
│   ├── merge/MergeFragment.java      – Merge PDFs
│   ├── split/SplitFragment.java      – Split PDF
│   ├── compress/CompressFragment.java – Compress PDF
│   ├── imagetopdf/ImageToPdfFragment – Images → PDF
│   ├── viewer/PdfViewerActivity.java  – PDF Viewer
│   ├── recent/RecentFilesFragment.java – Recent files
│   └── settings/SettingsFragment.java – Settings
├── adapters/
│   ├── MergeFileAdapter.java   – Draggable PDF list
│   ├── ImagePickerAdapter.java – Draggable image list
│   └── RecentFilesAdapter.java – Recent file list
├── utils/
│   ├── FileUtil.java           – File helpers
│   ├── PdfMergeUtil.java       – PdfBox merge
│   ├── PdfSplitUtil.java       – PdfBox split
│   ├── PdfCompressUtil.java    – PdfBox compress
│   └── ImageToPdfUtil.java     – Native PDF API
├── models/
│   ├── PdfFile.java            – Selected file model
│   └── RecentFile.java         – Room entity
└── database/
    ├── AppDatabase.java        – Room singleton
    └── RecentFileDao.java      – DAO interface
```

## Output Files
All generated PDFs are saved to:
`Android/data/com.quickpdf.app/files/Documents/QuickPDF/`

## Notes
- Permissions are handled automatically for Android 6–14
- All processing is done **offline**, files never leave the device
- Dark mode is persisted across app restarts via SharedPreferences
