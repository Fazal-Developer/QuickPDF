package com.hhhdeveloper.swiftpdf;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.hhhdeveloper.swiftpdf.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 1. Logo Card Spring Animation — bouncy scale in
        binding.cardSplashLogo.setScaleX(0.3f);
        binding.cardSplashLogo.setScaleY(0.3f);
        binding.cardSplashLogo.setAlpha(0f);
        binding.cardSplashLogo.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(900)
                .setInterpolator(new OvershootInterpolator(1.8f))
                .start();

        // 2. App Name slide up
        binding.tvSplashTitle.setAlpha(0f);
        binding.tvSplashTitle.setTranslationY(24f);
        binding.tvSplashTitle.animate()
                .alpha(1.0f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(250)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 3. Subtitle slide up
        binding.tvSplashSubtitle.setAlpha(0f);
        binding.tvSplashSubtitle.setTranslationY(20f);
        binding.tvSplashSubtitle.animate()
                .alpha(1.0f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(350)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 4. Tagline slide up
        binding.tvSplashTagline.setAlpha(0f);
        binding.tvSplashTagline.setTranslationY(20f);
        binding.tvSplashTagline.animate()
                .alpha(1.0f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(450)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 5. Progress bar — fade in then animate 0 → 72% like reference
        binding.pbSplashLoader.setAlpha(0f);
        binding.pbSplashLoader.setProgress(0);
        binding.tvSplashPercent.setAlpha(0f);
        binding.pbSplashLoader.animate()
                .alpha(1.0f)
                .setDuration(500)
                .setStartDelay(500)
                .withEndAction(() -> {
                    // Animate progress 0 → 72
                    ObjectAnimator progressAnim = ObjectAnimator.ofInt(
                            binding.pbSplashLoader, "progress", 0, 72);
                    progressAnim.setDuration(900);
                    progressAnim.setInterpolator(new DecelerateInterpolator());
                    progressAnim.start();
                    // Update percent text
                    binding.tvSplashPercent.animate().alpha(1.0f).setDuration(300).start();
                    progressAnim.addUpdateListener(animation -> {
                        int val = (int) animation.getAnimatedValue();
                        binding.tvSplashPercent.setText(val + "%");
                    });
                })
                .start();

        // Navigate to MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1900);
    }
}
