package com.hhhdeveloper.swiftpdf.ui.cv;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.adapters.CvTemplateAdapter;
import com.hhhdeveloper.swiftpdf.databinding.FragmentCvTemplateGalleryBinding;
import com.hhhdeveloper.swiftpdf.models.cv.CvModel;
import com.hhhdeveloper.swiftpdf.models.cv.CvTemplate;
import com.hhhdeveloper.swiftpdf.models.cv.PersonalInfo;
import com.hhhdeveloper.swiftpdf.ui.cv.views.CvPreviewView;

import java.util.ArrayList;
import java.util.List;

public class CvTemplateGalleryFragment extends Fragment {

    private FragmentCvTemplateGalleryBinding binding;
    private CvTemplateAdapter adapter;
    private List<CvTemplate> currentTemplates;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCvTemplateGalleryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Toolbar back
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Setup Templates Grid (2 columns on phone/tablet)
        currentTemplates = new ArrayList<>(CvTemplateRegistry.getAllTemplates());
        adapter = new CvTemplateAdapter(currentTemplates, template -> {
            showPremiumTemplatePreviewModal(template);
        });

        binding.rvTemplates.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvTemplates.setAdapter(adapter);

        // Setup Category Filter Chips
        binding.chipAll.setOnClickListener(v -> filterCategory("All"));
        binding.chipAts.setOnClickListener(v -> filterCategory("ATS Friendly"));
        binding.chipProfessional.setOnClickListener(v -> filterCategory("Professional"));
        binding.chipModern.setOnClickListener(v -> filterCategory("Modern"));
        binding.chipCreative.setOnClickListener(v -> filterCategory("Creative"));
        binding.chipAcademic.setOnClickListener(v -> filterCategory("Academic"));

        // Setup Search Filter TextWatcher
        binding.etSearchTemplates.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showPremiumTemplatePreviewModal(CvTemplate template) {
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

        // Render full sample preview
        CvModel previewModel = new CvModel();
        previewModel.setSelectedTemplateId(template.getId());
        previewModel.setPersonalInfo(new PersonalInfo("Hamza Fazal", "Software Engineer & Mobile Developer", "hamzafazal@deesu.pk", "+92 300 1234567", "Islamabad, Pakistan", "linkedin.com/in/hamzafazal", "hamzafazal.deesu.pk"));
        previewModel.setSummary("Software Engineering student and multi-disciplinary developer who loves building Android apps.");
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

    private void filterCategory(String category) {
        currentTemplates = CvTemplateRegistry.filterByCategory(category);
        adapter.updateTemplates(currentTemplates);
    }

    private void filterSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.updateTemplates(currentTemplates);
            return;
        }
        List<CvTemplate> searchList = new ArrayList<>();
        for (CvTemplate t : currentTemplates) {
            if (t.getName().toLowerCase().contains(query.toLowerCase()) ||
                t.getDescription().toLowerCase().contains(query.toLowerCase())) {
                searchList.add(t);
            }
        }
        adapter.updateTemplates(searchList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
