package com.hhhdeveloper.swiftpdf.ui.imagetopdf;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.databinding.ActivityImageEditorBinding;
import com.hhhdeveloper.swiftpdf.utils.ImageProcessUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageEditorActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_EDITED_URI = "extra_edited_uri";

    private ActivityImageEditorBinding binding;
    private Uri sourceUri;
    private Bitmap originalBitmap;
    private Bitmap processedBitmap;
    private float currentRotation = 0f;
    private int selectedFilterId = R.id.chip_filter_original;
    private boolean isCropEnabled = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() == null || !getIntent().hasExtra(EXTRA_IMAGE_URI)) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sourceUri = Uri.parse(getIntent().getStringExtra(EXTRA_IMAGE_URI));

        // Load image
        loadBitmap();

        // Bind clicks
        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveEditedImage());
        binding.btnRotateCw.setOnClickListener(v -> rotate90());
        binding.btnCropToggle.setOnClickListener(v -> toggleCropMode());

        // Chip group listener
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = group.getCheckedChipId();
            if (checkedId != -1) {
                selectedFilterId = checkedId;
                applyFilterAndDisplay();
            }
        });
    }

    private void loadBitmap() {
        executor.submit(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(sourceUri);
                // Downsample large images slightly to prevent OutOfMemoryError in editor workspace
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                // If the bitmap is extremely large, downsample to fit screen resolution max (e.g. 2000px max edge)
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(getContentResolver().openInputStream(sourceUri), null, options);
                
                int maxEdge = Math.max(options.outWidth, options.outHeight);
                if (maxEdge > 2400) {
                    options.inSampleSize = 2;
                }
                options.inJustDecodeBounds = false;

                originalBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(sourceUri), null, options);
                processedBitmap = originalBitmap;

                runOnUiThread(() -> {
                    if (originalBitmap != null) {
                        applyFilterAndDisplay();
                    } else {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void rotate90() {
        currentRotation = (currentRotation + 90f) % 360f;
        applyFilterAndDisplay();
        
        // Reset crop overlay size to match new dimensions
        if (isCropEnabled) {
            setupCropBounds();
        }
    }

    private void applyFilterAndDisplay() {
        if (originalBitmap == null) return;

        executor.submit(() -> {
            // 1. First rotate the original bitmap
            Bitmap rotated = ImageProcessUtil.rotate(originalBitmap, currentRotation);
            
            // 2. Next apply selected filter effect
            Bitmap filtered;
            if (selectedFilterId == R.id.chip_filter_grayscale) {
                filtered = ImageProcessUtil.toGrayscale(rotated);
            } else if (selectedFilterId == R.id.chip_filter_doc_scan) {
                filtered = ImageProcessUtil.toDocScan(rotated);
            } else if (selectedFilterId == R.id.chip_filter_enhance) {
                filtered = ImageProcessUtil.toEnhanced(rotated);
            } else {
                filtered = rotated;
            }

            processedBitmap = filtered;

            runOnUiThread(() -> {
                binding.ivEditorPreview.setImageBitmap(processedBitmap);
                
                // If crop is active, realign bounds
                if (isCropEnabled) {
                    setupCropBounds();
                }
            });
        });
    }

    private void toggleCropMode() {
        isCropEnabled = !isCropEnabled;
        if (isCropEnabled) {
            binding.cropOverlayView.setVisibility(View.VISIBLE);
            binding.btnCropToggle.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.holo_orange_dark)));
            setupCropBounds();
        } else {
            binding.cropOverlayView.setVisibility(View.GONE);
            binding.btnCropToggle.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(ColorStateListHelper()));
        }
    }

    private int ColorStateListHelper() {
        // Return default dark gray color for toggle off state
        return android.graphics.Color.parseColor("#2C2C2C");
    }

    private void setupCropBounds() {
        if (processedBitmap == null) return;
        
        // We must delay setup until view layout runs and has dimensions
        binding.cropOverlayView.post(() -> {
            int viewW = binding.cropOverlayView.getWidth();
            int viewH = binding.cropOverlayView.getHeight();
            int bmpW = processedBitmap.getWidth();
            int bmpH = processedBitmap.getHeight();

            if (viewW > 0 && viewH > 0) {
                RectF bounds = ImageProcessUtil.getBitmapRectInImageView(viewW, viewH, bmpW, bmpH);
                binding.cropOverlayView.setCropRect(bounds);
            }
        });
    }

    private void saveEditedImage() {
        if (processedBitmap == null) {
            finish();
            return;
        }

        executor.submit(() -> {
            try {
                Bitmap finalBitmap = processedBitmap;

                if (isCropEnabled) {
                    // Extract relative crop overlay rect
                    RectF cropRect = binding.cropOverlayView.getNormalizedCropRect(); // relative to view size [0..1]
                    
                    int viewW = binding.cropOverlayView.getWidth();
                    int viewH = binding.cropOverlayView.getHeight();
                    int bmpW = processedBitmap.getWidth();
                    int bmpH = processedBitmap.getHeight();
                    
                    // Actual bounding rect of bitmap in imageview
                    RectF bitmapRect = ImageProcessUtil.getBitmapRectInImageView(viewW, viewH, bmpW, bmpH);

                    // Reconstruct crop bounds relative to the actual bitmap
                    float scaleX = viewW / bitmapRect.width();
                    float scaleY = viewH / bitmapRect.height();
                    
                    // Coordinates of cropRect relative to bitmap bounds
                    float cropLeft = (cropRect.left * viewW - bitmapRect.left) / bitmapRect.width();
                    float cropTop = (cropRect.top * viewH - bitmapRect.top) / bitmapRect.height();
                    float cropRight = (cropRect.right * viewW - bitmapRect.left) / bitmapRect.width();
                    float cropBottom = (cropRect.bottom * viewH - bitmapRect.top) / bitmapRect.height();
                    
                    RectF normalizedRect = new RectF(
                            Math.max(0f, cropLeft),
                            Math.max(0f, cropTop),
                            Math.min(1f, cropRight),
                            Math.min(1f, cropBottom)
                    );

                    finalBitmap = ImageProcessUtil.crop(processedBitmap, normalizedRect);
                }

                // Save final cropped bitmap to temp file in cache
                File cacheFile = new File(getCacheDir(), "edited_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream fos = new FileOutputStream(cacheFile);
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
                fos.close();

                Uri outputUri = Uri.fromFile(cacheFile);
                runOnUiThread(() -> {
                    Intent result = new Intent();
                    result.putExtra(EXTRA_EDITED_URI, outputUri.toString());
                    setResult(RESULT_OK, result);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to save edit: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}
