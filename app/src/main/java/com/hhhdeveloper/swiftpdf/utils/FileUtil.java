package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtil {

    public static final String OUTPUT_DIR = "SwiftPDF";

    /**
     * Returns (or creates) the app's output directory in external files.
     */
    public static File getOutputDirectory(Context context) {
        return getOutputDirectory(context, null);
    }

    /**
     * Returns (or creates) a specific subfolder inside the app's output directory.
     * Features automatic fail-safe fallback to app-specific external storage if public Documents is restricted.
     */
    public static File getOutputDirectory(Context context, String subfolderName) {
        File baseDir = null;
        try {
            baseDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), OUTPUT_DIR);
            File targetDir = subfolderName != null ? new File(baseDir, subfolderName) : baseDir;
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created && !targetDir.exists()) {
                    baseDir = null; // Fail-safe fallback required
                }
            }
        } catch (Exception e) {
            baseDir = null;
        }

        // Guaranteed writable fallback on ALL Android versions (Scoped Storage compliant)
        if (baseDir == null) {
            File appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (appDir == null) appDir = context.getFilesDir();
            baseDir = new File(appDir, OUTPUT_DIR);
        }

        File targetDir = subfolderName != null ? new File(baseDir, subfolderName) : baseDir;
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        return targetDir;
    }

    /**
     * Returns (or creates) the SwiftPDF folder inside the Pictures directory.
     */
    public static File getPicturesOutputDirectory(Context context) {
        File baseDir = null;
        try {
            baseDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), OUTPUT_DIR);
            if (!baseDir.exists()) {
                boolean created = baseDir.mkdirs();
                if (!created && !baseDir.exists()) {
                    baseDir = null;
                }
            }
        } catch (Exception e) {
            baseDir = null;
        }

        if (baseDir == null) {
            File appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (appDir == null) appDir = context.getFilesDir();
            baseDir = new File(appDir, OUTPUT_DIR);
        }

        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        return baseDir;
    }

    /**
     * Generate a unique output file name inside its corresponding action subfolder.
     */
    public static File createOutputFile(Context context, String prefix) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        
        String subfolder;
        String extension = ".pdf";
        
        switch (prefix) {
            case "Merge":
            case "Merged":
                subfolder = "Merged";
                break;
            case "Split":
                subfolder = "Split";
                break;
            case "Compress":
            case "Compressed":
                subfolder = "Compressed";
                break;
            case "ImgToPdf":
            case "Word":
            case "Excel":
            case "PPT":
            case "Text":
                subfolder = "Converted";
                break;
            case "Watermark":
            case "Clean":
            case "RemoveWatermark":
                subfolder = "Watermarked";
                break;
            case "Protect":
            case "Unlock":
            case "Lock":
                subfolder = "Secured";
                break;
            case "Edit":
                subfolder = "Editor";
                break;
            default:
                subfolder = "Others";
                break;
        }

        String fileName = prefix + "_" + timestamp + extension;
        return new File(getOutputDirectory(context, subfolder), fileName);
    }

    /**
     * Copy a URI stream to a temp file for processing.
     */
    public static File copyUriToTempFile(Context context, Uri uri) throws IOException {
        String fileName = getFileName(context, uri);
        if (fileName == null) fileName = "temp_" + System.currentTimeMillis() + ".pdf";
        File tempFile = new File(context.getCacheDir(), fileName);
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(tempFile)) {
            if (in == null) throw new IOException("Cannot open input stream for: " + uri);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return tempFile;
    }

    /**
     * Get a file's display name from a URI.
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    /**
     * Get file size in bytes from a URI.
     */
    public static long getFileSize(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIdx >= 0) return cursor.getLong(sizeIdx);
            }
        }
        return 0;
    }

    /**
     * Get a shareable URI via FileProvider.
     */
    public static Uri getShareUri(Context context, File file) {
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".provider", file);
    }

    /**
     * Format file size for display.
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0);
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024));
    }

    /**
     * Format a timestamp for display.
     */
    public static String formatDate(long millis) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(millis));
    }

    /**
     * Delete a file safely.
     */
    public static boolean deleteFile(String path) {
        if (path == null) return false;
        File file = new File(path);
        return file.exists() && file.delete();
    }

    /**
     * Rename a file safely.
     */
    public static boolean renameFile(String oldPath, String newName) {
        File oldFile = new File(oldPath);
        if (!oldFile.exists()) return false;
        String ext = "";
        if (!newName.endsWith(".pdf")) ext = ".pdf";
        File newFile = new File(oldFile.getParent(), newName + ext);
        return oldFile.renameTo(newFile);
    }

    /**
     * Pre-create all SwiftPDF operational directories on startup.
     */
    public static void initializeDirectories(Context context) {
        String[] subfolders = {"Merged", "Split", "Compressed", "Converted", "Watermarked", "Secured", "Editor"};
        for (String sub : subfolders) {
            getOutputDirectory(context, sub);
        }
    }

    /**
     * Launch intent to open a specific subfolder inside the app's output directory.
     */
    public static void openOutputDirectory(Context context, String subfolderName) {
        try {
            String relativePath = "Documents/" + OUTPUT_DIR;
            if (subfolderName != null) {
                relativePath += "/" + subfolderName;
            }
            String docId = "primary:" + relativePath;
            Uri uri = android.provider.DocumentsContract.buildTreeDocumentUri(
                    "com.android.externalstorage.documents", docId);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "vnd.android.document/directory");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                File dir = getOutputDirectory(context, subfolderName);
                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", dir);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "vnd.android.document/directory");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ex) {
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    Uri uri = Uri.fromFile(getOutputDirectory(context, subfolderName));
                    intent.setDataAndType(uri, "*/*");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception exc) {
                    Toast.makeText(context, "No file manager found to open folder", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Notify the media scanner about a new file or folder so it shows up in system recents/gallery.
     */
    public static void scanSavedFile(Context context, File file) {
        if (file == null || !file.exists()) return;
        try {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        scanSavedFile(context, child);
                    }
                }
            } else {
                android.media.MediaScannerConnection.scanFile(context,
                        new String[]{file.getAbsolutePath()}, null,
                        (path, uri) -> {
                            // Scan completed successfully
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
