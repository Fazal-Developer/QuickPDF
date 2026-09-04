package com.hhhdeveloper.swiftpdf.ui.convert;

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
import com.hhhdeveloper.swiftpdf.databinding.FragmentConvertBinding;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.utils.ExcelToPdfUtil;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;
import com.hhhdeveloper.swiftpdf.utils.PdfToImageUtil;
import com.hhhdeveloper.swiftpdf.utils.PdfToTextUtil;
import com.hhhdeveloper.swiftpdf.utils.PptxToPdfUtil;
import com.hhhdeveloper.swiftpdf.utils.WordToPdfUtil;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BaseConvertFragment extends Fragment {

    private FragmentConvertBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String type; // "WORD", "EXCEL", "PPT", "TXT", "PDF_TO_TXT", "PDF_TO_IMG"
    private Uri selectedFileUri;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null && result.getData().getData() != null) {
                    selectedFileUri = result.getData().getData();
                    binding.tvSelectedFileName.setText(FileUtil.getFileName(requireContext(), selectedFileUri));
                    binding.btnActionConvert.setVisibility(View.VISIBLE);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getString("type", "WORD");
        } else {
            type = "WORD";
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentConvertBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI();

        binding.btnSelectFile.setOnClickListener(v -> triggerPicker());
        binding.btnActionConvert.setOnClickListener(v -> startConversionProcess());
    }

    private void setupUI() {
        switch (type) {
            case "WORD":
                binding.tvBannerTitle.setText("Word to PDF");
                binding.tvBannerSubtitle.setText("Convert Word documents (.docx) to PDF offline.");
                binding.tvPickerLabel.setText("Select Word Document");
                binding.tvSelectedFileName.setText("Select .docx file");
                binding.btnActionConvert.setText("Convert to PDF");
                break;
            case "EXCEL":
                binding.tvBannerTitle.setText("Excel to PDF");
                binding.tvBannerSubtitle.setText("Convert Excel spreadsheets (.xlsx) to PDF offline.");
                binding.tvPickerLabel.setText("Select Excel Spreadsheet");
                binding.tvSelectedFileName.setText("Select .xlsx file");
                binding.btnActionConvert.setText("Convert to PDF");
                break;
            case "PPT":
                binding.tvBannerTitle.setText("PowerPoint to PDF");
                binding.tvBannerSubtitle.setText("Convert slide presentations (.pptx) to PDF offline.");
                binding.tvPickerLabel.setText("Select slide presentation");
                binding.tvSelectedFileName.setText("Select .pptx file");
                binding.btnActionConvert.setText("Convert to PDF");
                break;
            case "TXT":
                binding.tvBannerTitle.setText("Text to PDF");
                binding.tvBannerSubtitle.setText("Convert plain text files (.txt) to PDF offline.");
                binding.tvPickerLabel.setText("Select plain text");
                binding.tvSelectedFileName.setText("Select .txt file");
                binding.btnActionConvert.setText("Convert to PDF");
                break;
            case "PDF_TO_TXT":
                binding.tvBannerTitle.setText("PDF to Text");
                binding.tvBannerSubtitle.setText("Extract all text content from a PDF document.");
                binding.tvPickerLabel.setText("Select PDF document");
                binding.tvSelectedFileName.setText("Select PDF file");
                binding.btnActionConvert.setText("Extract text content");
                break;
            case "PDF_TO_IMG":
                binding.tvBannerTitle.setText("PDF to Image");
                binding.tvBannerSubtitle.setText("Export PDF pages into individual JPEG images.");
                binding.tvPickerLabel.setText("Select PDF document");
                binding.tvSelectedFileName.setText("Select PDF file");
                binding.btnActionConvert.setText("Export pages to JPEGs");
                break;
        }
    }

    private void triggerPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        switch (type) {
            case "WORD":
                intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                break;
            case "EXCEL":
                intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            case "PPT":
                intent.setType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
                break;
            case "TXT":
                intent.setType("text/plain");
                break;
            case "PDF_TO_TXT":
            case "PDF_TO_IMG":
                intent.setType("application/pdf");
                break;
        }
        
        filePickerLauncher.launch(intent);
    }

    private void startConversionProcess() {
        if (selectedFileUri == null) return;

        ProgressDialog dialog = new ProgressDialog(requireContext());
        dialog.setMessage("Processing conversion...");
        dialog.setCancelable(false);
        dialog.show();

        executor.submit(() -> {
            try {
                // Copy selected file to cache
                File cacheFile = FileUtil.copyUriToTempFile(requireContext(), selectedFileUri);

                File outputFile = null;
                String operation = "CONVERT";

                switch (type) {
                    case "WORD": {
                        List<String> paragraphs = WordToPdfUtil.extractTextFromDocx(cacheFile);
                        if (paragraphs.isEmpty()) throw new Exception("Word document contains no readable text runs.");
                        outputFile = FileUtil.createOutputFile(requireContext(), "Word");
                        WordToPdfUtil.convertTextToPdf(paragraphs, outputFile);
                        break;
                    }
                    case "EXCEL": {
                        outputFile = FileUtil.createOutputFile(requireContext(), "Excel");
                        ExcelToPdfUtil.convertExcelToPdf(cacheFile, outputFile);
                        break;
                    }
                    case "PPT": {
                        outputFile = FileUtil.createOutputFile(requireContext(), "PPT");
                        PptxToPdfUtil.convertPptxToPdf(cacheFile, outputFile);
                        break;
                    }
                    case "TXT": {
                        List<String> paragraphs = WordToPdfUtil.extractTextFromTxt(cacheFile);
                        if (paragraphs.isEmpty()) throw new Exception("Text file contains no text.");
                        outputFile = FileUtil.createOutputFile(requireContext(), "Text");
                        WordToPdfUtil.convertTextToPdf(paragraphs, outputFile);
                        break;
                    }
                    case "PDF_TO_TXT": {
                        String outName = cacheFile.getName().replace(".pdf", "") + "_Extracted.txt";
                        outputFile = PdfToTextUtil.convertPdfToText(requireContext(), cacheFile, outName);
                        operation = "TXT_EXPORT";
                        break;
                    }
                    case "PDF_TO_IMG": {
                        List<File> images = PdfToImageUtil.convertPdfToImages(requireContext(), cacheFile);
                        if (images.isEmpty()) throw new Exception("PDF contains no renderable pages.");
                        outputFile = images.get(0).getParentFile(); // Output folder containing the images
                        break;
                    }
                }

                if (outputFile == null) {
                    throw new Exception("Failed to produce output files.");
                }

                // Register file in recents list if it's a file (not folder)
                if (outputFile.isFile()) {
                    RecentFile recent = new RecentFile(outputFile.getName(), outputFile.getAbsolutePath(),
                            outputFile.length(), System.currentTimeMillis(), operation);
                    android.content.Context ctx = getContext(); if (ctx == null) return; AppDatabase.getInstance(ctx.getApplicationContext()).recentFileDao().insert(recent);
                    com.hhhdeveloper.swiftpdf.utils.FileUtil.scanSavedFile(requireContext(), outputFile);
                } else if (outputFile.isDirectory()) {
                    com.hhhdeveloper.swiftpdf.utils.FileUtil.scanSavedFile(requireContext(), outputFile);
                }

                final File resultFile = outputFile;
                if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                    try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                    binding.btnActionConvert.setVisibility(View.GONE);
                    setupUI(); // Reset views
                    selectedFileUri = null;

                    Snackbar.make(binding.getRoot(), "Conversion successful!", Snackbar.LENGTH_LONG)
                            .setAction("Open Folder", v -> {
                                com.hhhdeveloper.swiftpdf.utils.FileUtil.openOutputDirectory(getContext(), "Converted");
                            }).show();
                });

            } catch (Exception e) {
                if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                    try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                    Toast.makeText(getContext(), "Processing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
