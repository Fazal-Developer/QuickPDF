package com.hhhdeveloper.swiftpdf.ui.cv;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.adapters.CvTemplateAdapter;
import com.hhhdeveloper.swiftpdf.databinding.FragmentCvLandingBinding;
import com.hhhdeveloper.swiftpdf.models.cv.CvModel;
import com.hhhdeveloper.swiftpdf.models.cv.CvTemplate;
import com.hhhdeveloper.swiftpdf.models.cv.PersonalInfo;
import com.hhhdeveloper.swiftpdf.ui.cv.views.CvPreviewView;

import java.util.List;

public class CvLandingFragment extends Fragment {

    private FragmentCvLandingBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCvLandingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Toolbar back button
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Primary: "Create New CV" -> Go to Template Gallery
        binding.btnCreateNewCv.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_cv_template_gallery);
        });

        // Secondary: "My CVs" -> Show My Saved CVs
        binding.btnMyCvs.setOnClickListener(v -> showMyCvsDialog());

        // Setup Featured Templates
        List<CvTemplate> templates = CvTemplateRegistry.getAllTemplates();
        CvTemplateAdapter adapter = new CvTemplateAdapter(templates, this::showTemplatePreviewSheet);

        binding.rvFeaturedTemplates.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvFeaturedTemplates.setAdapter(adapter);
    }

    private void showTemplatePreviewSheet(CvTemplate template) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.layout_cv_template_preview_sheet, null);
        dialog.setContentView(sheetView);

        TextView tvName = sheetView.findViewById(R.id.tv_preview_name);
        TextView tvDesc = sheetView.findViewById(R.id.tv_preview_desc);
        TextView tvAts = sheetView.findViewById(R.id.tv_badge_ats);
        TextView tvPhoto = sheetView.findViewById(R.id.tv_badge_photo);
        CvPreviewView previewCanvas = sheetView.findViewById(R.id.full_preview_canvas);
        View btnUse = sheetView.findViewById(R.id.btn_use_this_template);

        tvName.setText(template.getName());
        tvDesc.setText(template.getDescription());

        tvAts.setVisibility(template.isAtsFriendly() ? View.VISIBLE : View.GONE);
        tvPhoto.setVisibility(template.hasPhotoOption() ? View.VISIBLE : View.GONE);

        // Sample model for preview
        CvModel previewModel = new CvModel();
        previewModel.setSelectedTemplateId(template.getId());
        previewModel.setPersonalInfo(new PersonalInfo("Hamza Fazal", "Software Engineer & Mobile Developer", "hamzafazal@deesu.pk", "+92 300 1234567", "Islamabad, Pakistan", "linkedin.com/in/hamzafazal", "hamzafazal.deesu.pk"));
        previewModel.setSummary("Software Engineering student and multi-disciplinary developer passionate about building fast Android apps.");
        previewModel.getSkillsList().add("Android");
        previewModel.getSkillsList().add("Java");
        previewModel.getSkillsList().add("Python");
        previewCanvas.setCvModel(previewModel);

        btnUse.setOnClickListener(v -> {
            dialog.dismiss();
            Bundle args = new Bundle();
            args.putString("template_id", template.getId());
            Navigation.findNavController(requireView()).navigate(R.id.nav_cv_builder_steps, args);
        });

        dialog.show();
    }

    private void showMyCvsDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("📂 My Saved CVs")
                .setMessage("No saved drafts found yet. Tap 'Create New CV' to design your first professional resume!")
                .setPositiveButton("Create New CV", (dialog, which) -> {
                    Navigation.findNavController(requireView()).navigate(R.id.nav_cv_template_gallery);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
