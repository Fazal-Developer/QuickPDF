package com.hhhdeveloper.swiftpdf;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.hhhdeveloper.swiftpdf.databinding.ActivityMainBinding;
import com.hhhdeveloper.swiftpdf.ui.viewer.PdfViewerActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private NavController navController;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) { allGranted = false; break; }
                }
                if (!allGranted) {
                    Snackbar.make(binding.getRoot(),
                            getString(R.string.permission_required),
                            Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.grant_permission), v -> requestPermissions())
                            .show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Navigation — 4 top-level destinations (Home, Files, Tools, Settings)
        BottomNavigationView navView = binding.bottomNavView;
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_files, R.id.nav_tools, R.id.nav_settings)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        // Smooth tab switching listener — pops sub-destinations without glitches
        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() == itemId) {
                return true;
            }
            androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(R.id.nav_home, false, true)
                    .build();
            try {
                navController.navigate(itemId, null, navOptions);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        // Hide BottomNavigationView on sub-tool destinations for maximum vertical space
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.nav_home || id == R.id.nav_files || id == R.id.nav_tools || id == R.id.nav_settings) {
                binding.bottomNavView.setVisibility(View.VISIBLE);
            } else {
                binding.bottomNavView.setVisibility(View.GONE);
            }
        });

        // Setup Drawer
        binding.navView.setNavigationItemSelectedListener(this);
        binding.navView.setCheckedItem(R.id.drawer_home);

        // Handle PDF files opened from other apps
        handleIncomingIntent(getIntent());

        // Request storage permissions
        requestPermissions();

        // Check and prompt in-app review after 3 sessions
        com.hhhdeveloper.swiftpdf.utils.AppReviewManager.checkAndPromptReview(this);
    }

    /** Called from HomeFragment to open the drawer */
    public void openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.drawer_home) {
            navController.navigate(R.id.nav_home);
        } else if (id == R.id.drawer_tools) {
            navController.navigate(R.id.nav_tools);
        } else if (id == R.id.drawer_files) {
            navController.navigate(R.id.nav_files);
        } else if (id == R.id.drawer_settings) {
            navController.navigate(R.id.nav_settings);
        } else if (id == R.id.drawer_rate) {
            com.hhhdeveloper.swiftpdf.utils.AppReviewManager.showReviewDialog(this);
        } else if (id == R.id.drawer_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Check out SwiftPDF — Your smart PDF workspace!\nhttps://play.google.com/store/apps/details?id=" + getPackageName());
            startActivity(Intent.createChooser(shareIntent, "Share SwiftPDF"));
        } else if (id == R.id.drawer_more_apps) {
            com.hhhdeveloper.swiftpdf.utils.AppReviewManager.openMoreApps(this);
        } else if (id == R.id.drawer_privacy) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://deesu.org/privacy-policy-for-swiftpdf-pdf-editor-tools/")));
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                Intent viewerIntent = new Intent(this, PdfViewerActivity.class);
                viewerIntent.putExtra(PdfViewerActivity.EXTRA_PDF_URI, uri.toString());
                startActivity(viewerIntent);
            }
        }
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: handled via SAF per Google Play Policy
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (!permissions.isEmpty()) {
            permissionLauncher.launch(permissions.toArray(new String[0]));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
