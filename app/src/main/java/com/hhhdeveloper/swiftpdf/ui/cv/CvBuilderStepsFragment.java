package com.hhhdeveloper.swiftpdf.ui.cv;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hhhdeveloper.swiftpdf.database.AppDatabase;
import com.hhhdeveloper.swiftpdf.databinding.FragmentCvBuilderStepsBinding;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.models.cv.CertificationItem;
import com.hhhdeveloper.swiftpdf.models.cv.CvModel;
import com.hhhdeveloper.swiftpdf.models.cv.CvTemplate;
import com.hhhdeveloper.swiftpdf.models.cv.EducationItem;
import com.hhhdeveloper.swiftpdf.models.cv.ExperienceItem;
import com.hhhdeveloper.swiftpdf.models.cv.PersonalInfo;
import com.hhhdeveloper.swiftpdf.ui.cv.renderers.CvCanvasRenderer;
import com.hhhdeveloper.swiftpdf.ui.viewer.PdfViewerActivity;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CvBuilderStepsFragment extends Fragment {

    private FragmentCvBuilderStepsBinding binding;
    private CvModel cvModel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCvBuilderStepsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cvModel = new CvModel();

        if (getArguments() != null && getArguments().containsKey("template_id")) {
            String tid = getArguments().getString("template_id");
            cvModel.setSelectedTemplateId(tid);
        }

        // Toolbar back button
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Display locked selected template badge
        CvTemplate selectedTemplate = CvTemplateRegistry.getById(cvModel.getSelectedTemplateId());
        binding.btnTemplateSelector.setText("🔒 " + selectedTemplate.getName());
        binding.btnTemplateSelector.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Template locked to " + selectedTemplate.getName() + ". Tap Back to choose a different template.", Toast.LENGTH_SHORT).show();
        });

        // Action Buttons: Load Example & Clear Data
        binding.btnLoadExample.setOnClickListener(v -> loadExampleData());
        binding.btnClearData.setOnClickListener(v -> clearAllData());

        // Pre-fill sample profile data
        loadExampleData();

        // Setup TextWatchers for Live Instant Preview
        setupTextWatchers();

        // AI Improve Button
        binding.btnAiImprove.setOnClickListener(v -> improveSummaryWithAi());

        // Zoom Controls
        binding.btnZoomIn.setOnClickListener(v -> {
            float z = binding.previewCanvas.getZoomScale() + 0.1f;
            binding.previewCanvas.setZoomScale(z);
            binding.tvZoomLevel.setText((int)(z * 100) + "%");
        });
        binding.btnZoomOut.setOnClickListener(v -> {
            float z = binding.previewCanvas.getZoomScale() - 0.1f;
            binding.previewCanvas.setZoomScale(z);
            binding.tvZoomLevel.setText((int)(z * 100) + "%");
        });

        // Mobile View Mode Toggle (Edit | Preview)
        binding.toggleViewMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == binding.btnModeEdit.getId()) {
                binding.panelEditor.setVisibility(View.VISIBLE);
                binding.panelPreview.setVisibility(View.GONE);
            } else {
                binding.panelEditor.setVisibility(View.GONE);
                binding.panelPreview.setVisibility(View.VISIBLE);
            }
        });

        // Download PDF Button
        binding.btnDownloadPdf.setOnClickListener(v -> showExportQualityDialog());

        // Initial Paint
        notifyModelChanged();
    }

    private void loadExampleData() {
        binding.etFullName.setText("Hamza Fazal");
        binding.etJobTitle.setText("Senior Full Stack Developer & UI/UX Designer");
        binding.etEmail.setText("hamza.fazal@email.com");
        binding.etPhone.setText("+92 300 1234567");
        binding.etLocation.setText("Lahore, Pakistan");
        binding.etLinkedin.setText("linkedin.com/in/hamzafazal");

        binding.etSummary.setText("Innovative Full Stack Developer with 7+ years of experience building scalable web applications and mobile apps. Expertise in React, Node.js, and cloud architecture. Passionate about clean code, performance optimization, and user experience.");

        binding.etExperienceCompany.setText("TechVision Solutions");
        binding.etExperienceTitle.setText("Senior Full Stack Developer");
        binding.etExperienceDates.setText("Mar 2021 – Present");
        binding.etExperienceDesc.setText("Architected SaaS platform serving 100k+ users\nLed development team implementing CI/CD pipelines\nReduced application load time by 60% through code optimization");

        binding.etDegree.setText("B.Sc. Computer Science");
        binding.etSchool.setText("Lahore University of Management Sciences (LUMS)");
        binding.etEduYear.setText("2013 – 2017");

        binding.etSkills.setText("JavaScript, TypeScript, React, Node.js, Python, AWS, Docker, Git, CI/CD, Figma");
        binding.etLanguages.setText("English (Fluent), Urdu (Native), Punjabi (Native)");
        binding.etCertifications.setText("AWS Certified Solutions Architect – Associate\nGoogle Cloud Professional Developer");

        syncFormToModel();
        notifyModelChanged();
    }

    private void clearAllData() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🗑️ Clear All CV Data?")
                .setMessage("Are you sure you want to clear all fields? This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    binding.etFullName.setText("");
                    binding.etJobTitle.setText("");
                    binding.etEmail.setText("");
                    binding.etPhone.setText("");
                    binding.etLocation.setText("");
                    binding.etLinkedin.setText("");
                    binding.etSummary.setText("");
                    binding.etExperienceCompany.setText("");
                    binding.etExperienceTitle.setText("");
                    binding.etExperienceDates.setText("");
                    binding.etExperienceDesc.setText("");
                    binding.etDegree.setText("");
                    binding.etSchool.setText("");
                    binding.etEduYear.setText("");
                    binding.etSkills.setText("");
                    binding.etLanguages.setText("");
                    binding.etCertifications.setText("");
                    syncFormToModel();
                    notifyModelChanged();
                    Toast.makeText(requireContext(), "Cleared all CV data", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupTextWatchers() {
        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                syncFormToModel();
                notifyModelChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.etFullName.addTextChangedListener(tw);
        binding.etJobTitle.addTextChangedListener(tw);
        binding.etEmail.addTextChangedListener(tw);
        binding.etPhone.addTextChangedListener(tw);
        binding.etLocation.addTextChangedListener(tw);
        binding.etLinkedin.addTextChangedListener(tw);

        binding.etSummary.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvCharCounter.setText(s.length() + " / 500");
                syncFormToModel();
                notifyModelChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etExperienceCompany.addTextChangedListener(tw);
        binding.etExperienceTitle.addTextChangedListener(tw);
        binding.etExperienceDates.addTextChangedListener(tw);
        binding.etExperienceDesc.addTextChangedListener(tw);

        binding.etDegree.addTextChangedListener(tw);
        binding.etSchool.addTextChangedListener(tw);
        binding.etEduYear.addTextChangedListener(tw);

        binding.etSkills.addTextChangedListener(tw);
        binding.etLanguages.addTextChangedListener(tw);
        binding.etCertifications.addTextChangedListener(tw);
    }

    private void improveSummaryWithAi() {
        String current = binding.etSummary.getText().toString().trim();
        String improved = "Innovative Full Stack Developer with 7+ years of experience building scalable web applications and mobile apps. Expertise in React, Node.js, and cloud architecture. Passionate about clean code, performance optimization, and user experience.";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("✨ AI Professional Summary Suggestion")
                .setMessage("Original:\n\"" + (current.isEmpty() ? "None" : current) + "\"\n\nSuggested:\n\"" + improved + "\"")
                .setPositiveButton("Apply AI Text", (dialog, which) -> {
                    binding.etSummary.setText(improved);
                })
                .setNegativeButton("Keep Current", null)
                .show();
    }

    private void syncFormToModel() {
        if (binding == null) return;

        PersonalInfo info = cvModel.getPersonalInfo();
        info.setFullName(binding.etFullName.getText().toString().trim());
        info.setTitle(binding.etJobTitle.getText().toString().trim());
        info.setEmail(binding.etEmail.getText().toString().trim());
        info.setPhone(binding.etPhone.getText().toString().trim());
        info.setLocation(binding.etLocation.getText().toString().trim());
        info.setLinkedin(binding.etLinkedin.getText().toString().trim());

        cvModel.setSummary(binding.etSummary.getText().toString().trim());

        // Experience List
        String expCompany = binding.etExperienceCompany.getText().toString().trim();
        String expTitle   = binding.etExperienceTitle.getText().toString().trim();
        String expDates   = binding.etExperienceDates.getText().toString().trim();
        String expDesc    = binding.etExperienceDesc.getText().toString().trim();

        List<ExperienceItem> expList = new ArrayList<>();
        if (!expTitle.isEmpty() || !expCompany.isEmpty()) {
            expList.add(new ExperienceItem(expTitle, expCompany, expDates, "", expDesc));
        }
        cvModel.setExperienceList(expList);

        // Education List
        String degree = binding.etDegree.getText().toString().trim();
        String school = binding.etSchool.getText().toString().trim();
        String year   = binding.etEduYear.getText().toString().trim();

        List<EducationItem> eduList = new ArrayList<>();
        if (!degree.isEmpty() || !school.isEmpty()) {
            eduList.add(new EducationItem(degree, school, year));
        }
        cvModel.setEducationList(eduList);

        // Skills List
        String skillsRaw = binding.etSkills.getText().toString().trim();
        List<String> skills = new ArrayList<>();
        if (!skillsRaw.isEmpty()) {
            for (String s : skillsRaw.split(",")) {
                if (!s.trim().isEmpty()) skills.add(s.trim());
            }
        }
        cvModel.setSkillsList(skills);

        // Certifications
        String certsRaw = binding.etCertifications.getText().toString().trim();
        List<CertificationItem> certList = new ArrayList<>();
        if (!certsRaw.isEmpty()) {
            certList.add(new CertificationItem(certsRaw, "", ""));
        }
        cvModel.setCertificationsList(certList);

        cvModel.setLastEdited(System.currentTimeMillis());
    }

    private void notifyModelChanged() {
        if (binding == null) return;
        binding.previewCanvas.setCvModel(cvModel);
    }

    private void showExportQualityDialog() {
        String[] options = {"Standard Quality (Smaller size)", "High Quality (Recommended)", "Best Quality (Print ready)"};
        final int[] selected = {1};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Export your CV 📄")
                .setMessage("Paper: A4  •  Format: Selectable Text PDF")
                .setSingleChoiceItems(options, selected[0], (dialog, which) -> selected[0] = which)
                .setPositiveButton("Download PDF", (dialog, which) -> exportPdf())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportPdf() {
        binding.btnDownloadPdf.setEnabled(false);
        binding.btnDownloadPdf.setText("Exporting...");

        executor.submit(() -> {
            try {
                int widthPt = (int) CvCanvasRenderer.A4_WIDTH_PT;
                int heightPt = (int) CvCanvasRenderer.A4_HEIGHT_PT;

                PdfDocument pdfDocument = new PdfDocument();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(widthPt, heightPt, 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                Canvas canvas = page.getCanvas();
                CvCanvasRenderer.render(canvas, cvModel, widthPt, heightPt);

                pdfDocument.finishPage(page);

                File outputFile = FileUtil.createOutputFile(requireContext(), "CV");
                FileOutputStream out = new FileOutputStream(outputFile);
                pdfDocument.writeTo(out);
                pdfDocument.close();
                out.close();

                RecentFile recent = new RecentFile(
                        outputFile.getName(),
                        outputFile.getAbsolutePath(),
                        outputFile.length(),
                        System.currentTimeMillis(),
                        "CV_BUILDER"
                );
                AppDatabase.getInstance(requireContext().getApplicationContext())
                        .recentFileDao().insert(recent);

                FileUtil.scanSavedFile(requireContext(), outputFile);

                if (getActivity() == null || !isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.btnDownloadPdf.setEnabled(true);
                    binding.btnDownloadPdf.setText("📄 PDF");
                    Toast.makeText(requireContext(), "CV PDF Exported Successfully!", Toast.LENGTH_SHORT).show();

                    Intent viewerIntent = new Intent(requireContext(), PdfViewerActivity.class);
                    viewerIntent.putExtra(PdfViewerActivity.EXTRA_PDF_PATH, outputFile.getAbsolutePath());
                    startActivity(viewerIntent);
                });

            } catch (Exception e) {
                if (getActivity() == null || !isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.btnDownloadPdf.setEnabled(true);
                    binding.btnDownloadPdf.setText("📄 PDF");
                    Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
