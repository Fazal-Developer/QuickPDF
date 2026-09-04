package com.hhhdeveloper.swiftpdf.ui.tools;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.databinding.FragmentToolsBinding;

public class ToolsFragment extends Fragment {

    private FragmentToolsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentToolsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Core Tools
        binding.toolCvMaker.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_cv_maker));
        binding.toolMerge.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_merge));
        binding.toolImageToPdf.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_image_to_pdf));
        binding.toolPdfToImage.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_pdf_to_image));
        binding.toolSplit.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_split));
        binding.toolCompress.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_compress));
        binding.toolSecurity.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_security));
        binding.toolUnlock.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_security));
        binding.toolWatermark.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_watermark));
        binding.toolRemoveWatermark.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_remove_watermark));

        // Convert Tools
        binding.toolWordToPdf.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_word_to_pdf));
        binding.toolExcelToPdf.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_excel_to_pdf));
        binding.toolPptToPdf.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_ppt_to_pdf));
        binding.toolTextToPdf.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_text_to_pdf));
        binding.toolPdfToText.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_pdf_to_text));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
