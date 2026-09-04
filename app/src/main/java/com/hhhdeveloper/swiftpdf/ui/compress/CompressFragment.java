package com.hhhdeveloper.swiftpdf.ui.compress;

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

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.database.AppDatabase;
import com.hhhdeveloper.swiftpdf.databinding.FragmentCompressBinding;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;
import com.hhhdeveloper.swiftpdf.utils.PdfCompressUtil;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompressFragment extends Fragment {

    private FragmentCompressBinding binding;
    private Uri selectedPdfUri;
    private File selectedPdfFile;
    private long originalSize;
    private int currentCompressionLevel = PdfCompressUtil.LEVEL_MEDIUM;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null || result.getData().getData() == null) return;
                selectedPdfUri = result.getData().getData();
                loadPdf();
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCompressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSelectPdf.setOnClickListener(v -> openFilePicker());
        binding.btnCompress.setOnClickListener(v -> startCompress());

        binding.chipGroupCompression.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_low)         currentCompressionLevel = PdfCompressUtil.LEVEL_LOW;
            else if (id == R.id.chip_medium) currentCompressionLevel = PdfCompressUtil.LEVEL_MEDIUM;
            else if (id == R.id.chip_high)   currentCompressionLevel = PdfCompressUtil.LEVEL_HIGH;
            updateEstimate();
        });

        // Default: medium selected
        binding.chipMedium.setChecked(true);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePicker.launch(intent);
    }

    private void loadPdf() {
        originalSize = FileUtil.getFileSize(requireContext(), selectedPdfUri);
        binding.tvOriginalSize.setText(FileUtil.formatFileSize(originalSize));
        String name = FileUtil.getFileName(requireContext(), selectedPdfUri);
        binding.tvSelectLabel.setText(name != null ? name : "PDF selected");
        binding.tvSelectSub.setText("Tap to change file");
        updateEstimate();

        executor.submit(() -> {
            try {
                selectedPdfFile = FileUtil.copyUriToTempFile(requireContext(), selectedPdfUri);
            } catch (Exception e) {
                if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to load PDF", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateEstimate() {
        if (originalSize <= 0) return;
        long estimated = PdfCompressUtil.estimateCompressedSize(originalSize, currentCompressionLevel);
        binding.tvEstimatedSize.setText(FileUtil.formatFileSize(estimated));
    }

    private void startCompress() {
        if (selectedPdfFile == null) {
            Toast.makeText(requireContext(), R.string.select_pdf, Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(requireContext());
        dialog.setMessage(getString(R.string.compressing_pdf));
        dialog.setCancelable(false);
        dialog.show();

        int level = currentCompressionLevel;
        executor.submit(() ->
            PdfCompressUtil.compress(requireContext(), selectedPdfFile, level,
                    new PdfCompressUtil.CompressCallback() {
                        @Override
                        public void onSuccess(File outputFile, long origSize, long compSize) {
                            saveToRecent(outputFile, "COMPRESS");
                            int reduction = PdfCompressUtil.getReductionPercent(origSize, compSize);
                            if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                                try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                                binding.tvEstimatedSize.setText(FileUtil.formatFileSize(compSize));
                                Snackbar.make(binding.getRoot(),
                                        getString(R.string.compress_success) +
                                                " (Reduced by " + reduction + "%)",
                                        Snackbar.LENGTH_LONG)
                                        .setAction("Open Folder", v -> {
                                            com.hhhdeveloper.swiftpdf.utils.FileUtil.openOutputDirectory(getContext(), "Compressed");
                                        })
                                        .show();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                                try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                                Toast.makeText(getContext(), R.string.compress_failed,
                                        Toast.LENGTH_LONG).show();
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
