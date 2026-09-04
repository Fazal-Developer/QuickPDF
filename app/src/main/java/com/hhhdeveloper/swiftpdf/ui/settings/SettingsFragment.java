package com.hhhdeveloper.swiftpdf.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private static final String PREFS_NAME = "prefs";
    private static final String PREF_DARK_MODE = "dark_mode";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load saved dark mode preference without triggering listener loop
        boolean darkMode = requireContext()
                .getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE)
                .getBoolean(PREF_DARK_MODE, false);

        binding.switchDarkMode.setOnCheckedChangeListener(null);
        binding.switchDarkMode.setChecked(darkMode);

        // Dark mode toggle (only fires on user click/press)
        binding.switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            if (!btn.isPressed()) return;
            requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_DARK_MODE, isChecked)
                    .apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        binding.cardOutputFolder.setOnClickListener(v -> {
            com.hhhdeveloper.swiftpdf.utils.FileUtil.openOutputDirectory(requireContext(), null);
        });

        // Rate app → In-App Rating Dialog
        binding.cardRateApp.setOnClickListener(v -> {
            com.hhhdeveloper.swiftpdf.utils.AppReviewManager.showReviewDialog(requireActivity());
        });

        // More Apps by D.TEN
        binding.cardMoreApps.setOnClickListener(v -> {
            com.hhhdeveloper.swiftpdf.utils.AppReviewManager.openMoreApps(requireContext());
        });

        // Share app
        binding.cardShareApp.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SwiftPDF - PDF Workspace");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Check out SwiftPDF – Your smart PDF workspace!\n" +
                    "https://play.google.com/store/apps/details?id=" +
                    requireContext().getPackageName());
            startActivity(Intent.createChooser(shareIntent, "Share SwiftPDF via"));
        });

        // About Us Activity
        binding.cardAbout.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), com.hhhdeveloper.swiftpdf.AboutActivity.class));
        });

        // App Language Selection
        binding.cardLanguage.setOnClickListener(v -> {
            String[] langNames = {"English", "Español (Spanish)", "Français (French)", "Deutsch (German)", "العربية (Arabic)", "اردو (Urdu)"};
            String[] langCodes = {"en", "es", "fr", "de", "ar", "ur"};
            
            androidx.core.os.LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
            String currentLang = locales.isEmpty() ? "en" : locales.get(0).getLanguage();
            int selectedIndex = 0;
            for (int i = 0; i < langCodes.length; i++) {
                if (langCodes[i].equals(currentLang)) {
                    selectedIndex = i;
                    break;
                }
            }

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_language)
                    .setSingleChoiceItems(langNames, selectedIndex, (dialog, which) -> {
                        try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                        String tag = langCodes[which];
                        AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(tag));
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        // Set Language Subtitle
        androidx.core.os.LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        String currentLang = locales.isEmpty() ? "en" : locales.get(0).getLanguage();
        String langLabel = "English";
        if (currentLang.equals("es")) langLabel = "Español";
        else if (currentLang.equals("fr")) langLabel = "Français";
        else if (currentLang.equals("de")) langLabel = "Deutsch";
        else if (currentLang.equals("ar")) langLabel = "العربية";
        else if (currentLang.equals("ur")) langLabel = "اردو";
        binding.tvLanguageSubtitle.setText(langLabel);

        // Cache Calculations
        updateCacheSize();
        binding.cardClearCache.setOnClickListener(v -> {
            clearCache();
        });

        // Help & FAQ Dialog
        binding.cardFaq.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.help_faq)
                    .setMessage(
                        "📌 Where are my files saved?\n" +
                        "All converted PDF files are saved directly in your shared \"Documents/SwiftPDF/\" directory. Converted images are stored inside \"Pictures/SwiftPDF/\".\n\n" +
                        "📌 Will other apps detect these files?\n" +
                        "Yes. Newly compiled PDFs appear instantly in the Android system \"Recent Files\" lists, and converted images are accessible in your phone's default Gallery app under the album folder named \"SwiftPDF\".\n\n" +
                        "📌 Are my files private and secure?\n" +
                        "Absolutely. SwiftPDF processes all operations locally and offline on your device. We do not upload your documents to any cloud server."
                    )
                    .setPositiveButton(R.string.ok, null)
                    .show();
        });
    }

    private void updateCacheSize() {
        try {
            long size = getDirSize(requireContext().getCacheDir());
            java.io.File extCache = requireContext().getExternalCacheDir();
            if (extCache != null) {
                size += getDirSize(extCache);
            }
            String sizeStr = com.hhhdeveloper.swiftpdf.utils.FileUtil.formatFileSize(size);
            binding.tvCacheSize.setText("Cache: " + sizeStr);
        } catch (Exception e) {
            binding.tvCacheSize.setText("Cache: 0 B");
        }
    }

    private long getDirSize(java.io.File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.isDirectory()) {
                        size += getDirSize(f);
                    } else {
                        size += f.length();
                    }
                }
            }
        } else if (dir != null) {
            size += dir.length();
        }
        return size;
    }

    private void clearCache() {
        deleteDirContent(requireContext().getCacheDir());
        java.io.File extCache = requireContext().getExternalCacheDir();
        if (extCache != null) {
            deleteDirContent(extCache);
        }
        updateCacheSize();
        android.widget.Toast.makeText(requireContext(), "Cache cleared successfully", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void deleteDirContent(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.isDirectory()) {
                        deleteDirContent(f);
                    }
                    f.delete();
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
