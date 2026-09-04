package com.hhhdeveloper.swiftpdf.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.hhhdeveloper.swiftpdf.MainActivity;
import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ---- Greeting ----
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12)      greeting = "Good morning \uD83D\uDC4B";
        else if (hour < 17) greeting = "Good afternoon \u2600\uFE0F";
        else                greeting = "Good evening \uD83C\uDF19";
        binding.tvGreeting.setText(greeting);

        // ---- Hamburger → open drawer ----
        binding.btnMenu.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });

        // ---- Search bar — navigates to Files/Recent tab with query ----
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                // If user types, navigate to Recent/Files tab where search is available
                if (s.length() > 0) {
                    if (isAdded() && getActivity() != null) {
                        Navigation.findNavController(requireView()).navigate(R.id.nav_recent);
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // ---- Quick Actions — same cards as the All Tools section below ----
        binding.cardMerge.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_merge));
        binding.cardImageToPdf.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_image_to_pdf));
        binding.cardCompress.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_compress));
        binding.cardSplit.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_split));

        // ---- See All Recent ----
        binding.tvSeeAllRecent.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_files));

        // ---- Recent Files — limit to 5 ----
        binding.rvRecentPreview.setLayoutManager(new LinearLayoutManager(getContext()));

        android.content.Context ctx = getContext();
        if (ctx != null) {
            com.hhhdeveloper.swiftpdf.database.AppDatabase db =
                    com.hhhdeveloper.swiftpdf.database.AppDatabase.getInstance(ctx.getApplicationContext());
            db.recentFileDao().getRecentFiles(5).observe(getViewLifecycleOwner(), topRecent -> {
                if (binding == null) return;
                if (topRecent == null || topRecent.isEmpty()) {
                    binding.rvRecentPreview.setVisibility(View.GONE);
                    binding.tvNoRecent.setVisibility(View.VISIBLE);
                } else {
                    binding.rvRecentPreview.setVisibility(View.VISIBLE);
                    binding.tvNoRecent.setVisibility(View.GONE);
                    com.hhhdeveloper.swiftpdf.adapters.RecentFilesAdapter adapter =
                            new com.hhhdeveloper.swiftpdf.adapters.RecentFilesAdapter(topRecent);
                    adapter.setOnFileActionListener(new com.hhhdeveloper.swiftpdf.adapters.RecentFilesAdapter.OnFileActionListener() {
                        @Override
                        public void onOpen(com.hhhdeveloper.swiftpdf.models.RecentFile file) {
                            if (getActivity() == null || !isAdded()) return;
                            Intent intent = new Intent(getContext(), com.hhhdeveloper.swiftpdf.ui.viewer.PdfViewerActivity.class);
                            intent.putExtra(com.hhhdeveloper.swiftpdf.ui.viewer.PdfViewerActivity.EXTRA_PDF_PATH, file.getFilePath());
                            startActivity(intent);
                        }
                        @Override public void onShare(com.hhhdeveloper.swiftpdf.models.RecentFile file) {}
                        @Override public void onRename(com.hhhdeveloper.swiftpdf.models.RecentFile file) {}
                        @Override public void onDelete(com.hhhdeveloper.swiftpdf.models.RecentFile file) {}
                        @Override public void onEditPages(com.hhhdeveloper.swiftpdf.models.RecentFile file) {}
                        @Override public void onLocate(com.hhhdeveloper.swiftpdf.models.RecentFile file) {}
                    });
                    binding.rvRecentPreview.setAdapter(adapter);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
