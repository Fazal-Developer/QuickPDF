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
import com.hhhdeveloper.swiftpdf.databinding.FragmentRemoveWatermarkBinding;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;
import com.hhhdeveloper.swiftpdf.utils.PdfWatermarkRemover;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoveWatermarkFragment extends Fragment {

    private FragmentRemoveWatermarkBinding binding;
    private Uri pdfUri;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null && result.getData().getData() != null) {
                    pdfUri = result.getData().getData();
                    binding.tvSelectedFileName.setText(FileUtil.getFileName(requireContext(), pdfUri));
                    binding.btnProcessRemove.setVisibility(View.VISIBLE);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRemoveWatermarkBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSelectPdf.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            filePickerLauncher.launch(intent);
        });

        binding.btnProcessRemove.setOnClickListener(v -> startWatermarkRemoval());
    }

    private void startWatermarkRemoval() {
        if (pdfUri == null) return;

        String query = binding.inputWatermarkText.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter watermark text query to search and remove", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(requireContext());
        dialog.setMessage("Processing PDF streams to remove watermark...");
        dialog.setCancelable(false);
        dialog.show();

        executor.submit(() -> {
            try {
                // Copy selected file to cache
                File cacheFile = FileUtil.copyUriToTempFile(requireContext(), pdfUri);

                String outName = cacheFile.getName().replace(".pdf", "") + "_Clean.pdf";
                File outputFile = PdfWatermarkRemover.removeWatermark(requireContext(), cacheFile, query, outName);

                // Save to database
                RecentFile recent = new RecentFile(outputFile.getName(), outputFile.getAbsolutePath(),
                        outputFile.length(), System.currentTimeMillis(), "CLEAN");
                android.content.Context ctx = getContext(); if (ctx == null) return; AppDatabase.getInstance(ctx.getApplicationContext()).recentFileDao().insert(recent);
                FileUtil.scanSavedFile(requireContext(), outputFile);

                if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                    try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                    binding.btnProcessRemove.setVisibility(View.GONE);
                    binding.tvSelectedFileName.setText(getString(R.string.select_pdf));
                    binding.inputWatermarkText.setText("");
                    pdfUri = null;

                    Snackbar.make(binding.getRoot(), "Watermark removed successfully!", Snackbar.LENGTH_LONG)
                            .setAction("Open Folder", v -> {
                                com.hhhdeveloper.swiftpdf.utils.FileUtil.openOutputDirectory(getContext(), "Watermarked");
                            }).show();
                });

            } catch (Exception e) {
                if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                    try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                    Toast.makeText(getContext(), "Failed to remove watermark: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        executor.shutdown();
    }
}
