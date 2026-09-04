package com.hhhdeveloper.swiftpdf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hhhdeveloper.swiftpdf.databinding.ActivityAboutBinding;

public class AboutActivity extends AppCompatActivity {

    private ActivityAboutBinding binding;
    public static final String HAMZA_WEBSITE = "https://hamzafazal.deesu.pk";
    public static final String WAZIR_WEBSITE = "https://drwazir.deesu.org/";
    public static final String PRIVACY_URL   = "https://deesu.org/privacy-policy-for-swiftpdf-pdf-editor-tools/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("About SwiftPDF");
        }

        // Hamza Fazal Website Link
        binding.btnHamzaWebsite.setOnClickListener(v -> openUrl(HAMZA_WEBSITE));

        // Dr. Wazir Ahmed Website Link
        binding.btnWazirWebsite.setOnClickListener(v -> openUrl(WAZIR_WEBSITE));

        // Privacy Policy Link
        binding.cardPrivacyPolicy.setOnClickListener(v -> openUrl(PRIVACY_URL));
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            // Fallback if browser app fails
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
