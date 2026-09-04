package com.hhhdeveloper.swiftpdf;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class QuickPdfApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Apply saved theme preference at app startup
        boolean darkMode = getSharedPreferences("prefs", MODE_PRIVATE)
                .getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // Initialize PdfBox-Android
        PDFBoxResourceLoader.init(getApplicationContext());
        // Initialize output directories
        com.hhhdeveloper.swiftpdf.utils.FileUtil.initializeDirectories(this);
    }
}
