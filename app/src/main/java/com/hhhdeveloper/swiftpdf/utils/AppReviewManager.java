package com.hhhdeveloper.swiftpdf.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hhhdeveloper.swiftpdf.R;

public class AppReviewManager {

    private static final String PREF_NAME           = "app_review_prefs";
    private static final String KEY_LAUNCH_COUNT    = "launch_count";
    private static final String KEY_USER_RESPONDED  = "user_responded";
    public static final String MORE_APPS_URL        = "https://play.google.com/store/apps/developer?id=Deesu+Training+%26+Education+Network+%28D.TEN%29";

    /**
     * Call on MainActivity startup. Increments launch count and presents review prompt after 3 sessions.
     */
    public static void checkAndPromptReview(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean responded = prefs.getBoolean(KEY_USER_RESPONDED, false);
        if (responded) return;

        int launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1;
        prefs.edit().putInt(KEY_LAUNCH_COUNT, launchCount).apply();

        if (launchCount >= 3) {
            showReviewDialog(activity);
        }
    }

    public static void showReviewDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        new MaterialAlertDialogBuilder(activity)
                .setTitle("⭐ Enjoying SwiftPDF?")
                .setMessage("If you love using SwiftPDF, please take a moment to rate us 5 stars on the Play Store. Your support means the world to us!")
                .setPositiveButton("Rate 5 Stars ⭐", (dialog, which) -> {
                    prefs.edit().putBoolean(KEY_USER_RESPONDED, true).apply();
                    openPlayStoreRating(activity);
                })
                .setNeutralButton("Remind Me Later", (dialog, which) -> {
                    // Reset launch count so it prompts again after 3 sessions
                    prefs.edit().putInt(KEY_LAUNCH_COUNT, 0).apply();
                })
                .setNegativeButton("No Thanks", (dialog, which) -> {
                    prefs.edit().putBoolean(KEY_USER_RESPONDED, true).apply();
                })
                .setCancelable(false)
                .show();
    }

    public static void openPlayStoreRating(Context context) {
        if (context == null) return;
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + context.getPackageName())));
        } catch (Exception e) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + context.getPackageName())));
            } catch (Exception ignored) {}
        }
    }

    public static void openMoreApps(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MORE_APPS_URL));
            context.startActivity(intent);
        } catch (Exception ignored) {}
    }
}
