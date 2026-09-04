package com.hhhdeveloper.swiftpdf.ui.watermark;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.snackbar.Snackbar;
import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.database.AppDatabase;
import com.hhhdeveloper.swiftpdf.databinding.FragmentWatermarkBinding;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;
import com.hhhdeveloper.swiftpdf.utils.PdfWatermarkUtil;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WatermarkFragment extends Fragment {

    private FragmentWatermarkBinding binding;
    private Uri selectedPdfUri;
    private File selectedPdfFile;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> filePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    selectedPdfUri = result.getData().getData();
                    loadPdf();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWatermarkBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSelectPdf.setOnClickListener(v -> openFilePicker());

        // Bind Sliders to Label updates
        binding.sliderTextSize.addOnChangeListener((slider, value, fromUser) ->
                binding.tvTextSizeLabel.setText("Text Size: " + (int) value + "sp"));

        binding.sliderRotation.addOnChangeListener((slider, value, fromUser) ->
                binding.tvRotationLabel.setText("Rotation Angle: " + (int) value + "°"));

        binding.btnWatermark.setOnClickListener(v -> applyWatermark());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePicker.launch(intent);
    }

    private void loadPdf() {
        String name = FileUtil.getFileName(requireContext(), selectedPdfUri);
        binding.tvSelectLabel.setText(name != null ? name : "PDF selected");
        binding.tvSelectSub.setText("Tap to change file");

        executor.submit(() -> {
            try {
                selectedPdfFile = FileUtil.copyUriToTempFile(requireContext(), selectedPdfUri);
            } catch (Exception e) {
                if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to load PDF", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void applyWatermark() {
        if (selectedPdfFile == null) {
            Toast.makeText(requireContext(), R.string.select_pdf, Toast.LENGTH_SHORT).show();
            return;
        }

        String text = binding.etWatermarkText.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "Watermark text cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        int textSize = (int) binding.sliderTextSize.getValue();
        float rotation = binding.sliderRotation.getValue();

        // Check color chip selection
        String hexColor = "#808080"; // Light Grey default
        int checkedChipId = binding.chipGroupColor.getCheckedChipId();
        if (checkedChipId == R.id.chip_red) {
            hexColor = "#E53935"; // Red
        } else if (checkedChipId == R.id.chip_blue) {
            hexColor = "#1E88E5"; // Blue
        }

        float opacity = 0.35f; // Standard subtle transparency for watermarks

        ProgressDialog dialog = new ProgressDialog(requireContext());
        dialog.setMessage(getString(R.string.watermarking_pdf));
        dialog.setCancelable(false);
        dialog.show();

        String finalHexColor = hexColor;
        executor.submit(() -> PdfWatermarkUtil.addWatermark(requireContext(), selectedPdfFile, text,
                textSize, rotation, finalHexColor, opacity, new PdfWatermarkUtil.WatermarkCallback() {
                    @Override
                    public void onSuccess(File outputFile) {
                        saveToRecent(outputFile, "WATERMARK");
                        if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                            try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                             Snackbar.make(binding.getRoot(), R.string.watermark_success, Snackbar.LENGTH_LONG)
                                     .setAction("Open Folder", v -> {
                                         com.hhhdeveloper.swiftpdf.utils.FileUtil.openOutputDirectory(getContext(), "Watermarked");
                                     })
                                     .show();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                            try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                            Toast.makeText(getContext(), R.string.watermark_failed, Toast.LENGTH_LONG).show();
                        });
                    }
                }));
    }

    private void saveToRecent(File file, String operation) {
        RecentFile recent = new RecentFile(file.getName(), file.getAbsolutePath(),
                file.length(), System.currentTimeMillis(), operation);
        executor.submit(() -> {
            android.content.Context ctx = getContext(); if (ctx == null) return; AppDatabase.getInstance(ctx.getApplicationContext()).recentFileDao().insert(recent);
            FileUtil.scanSavedFile(requireContext(), file);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
