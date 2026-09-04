package com.hhhdeveloper.swiftpdf.ui.split;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.adapters.MergeFileAdapter;
import com.hhhdeveloper.swiftpdf.database.AppDatabase;
import com.hhhdeveloper.swiftpdf.databinding.FragmentSplitBinding;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.ui.viewer.PdfViewerActivity;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;
import com.hhhdeveloper.swiftpdf.utils.PdfSplitUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplitFragment extends Fragment {

    private FragmentSplitBinding binding;
    private Uri selectedPdfUri;
    private File selectedPdfFile;
    private int totalPages = 0;
    private final List<Boolean> pageSelected = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Adapter for pages (we reuse a simple approach with a vertical list)
    private PageListAdapter pageAdapter;

    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null || result.getData().getData() == null) return;
                selectedPdfUri = result.getData().getData();
                loadPdf();
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSplitBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pageAdapter = new PageListAdapter(pageSelected, (pos, checked) -> pageSelected.set(pos, checked));
        binding.rvPages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPages.setAdapter(pageAdapter);

        binding.btnSelectPdf.setOnClickListener(v -> openFilePicker());

        binding.btnSelectAll.setOnClickListener(v -> {
            for (int i = 0; i < pageSelected.size(); i++) pageSelected.set(i, true);
            pageAdapter.notifyDataSetChanged();
        });

        binding.btnDeselectAll.setOnClickListener(v -> {
            for (int i = 0; i < pageSelected.size(); i++) pageSelected.set(i, false);
            pageAdapter.notifyDataSetChanged();
        });

        binding.btnSplit.setOnClickListener(v -> startSplit());
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
                totalPages = PdfSplitUtil.getPageCount(selectedPdfFile);

                pageSelected.clear();
                for (int i = 0; i < totalPages; i++) pageSelected.add(false);

                if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                    binding.tvPageCount.setText(totalPages + " pages");
                    pageAdapter.setPageCount(totalPages);
                    pageAdapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to load PDF", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void startSplit() {
        if (selectedPdfFile == null) {
            Toast.makeText(requireContext(), R.string.select_pdf, Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> selectedPages = new ArrayList<>();
        for (int i = 0; i < pageSelected.size(); i++) {
            if (pageSelected.get(i)) selectedPages.add(i);
        }

        if (selectedPages.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_pages_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(requireContext());
        dialog.setMessage(getString(R.string.splitting_pdf));
        dialog.setCancelable(false);
        dialog.show();

        PdfSplitUtil.splitByPages(requireContext(), selectedPdfFile, selectedPages,
                new PdfSplitUtil.SplitCallback() {
                    @Override
                    public void onSuccess(List<File> outputFiles) {
                        for (File f : outputFiles) saveToRecent(f, "SPLIT");
                        if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                            try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                            Snackbar.make(binding.getRoot(),
                                    outputFiles.size() + " file(s) created successfully!",
                                    Snackbar.LENGTH_LONG)
                                    .setAction("Open Folder", v -> {
                                        com.hhhdeveloper.swiftpdf.utils.FileUtil.openOutputDirectory(getContext(), "Split");
                                    }).show();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                            try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                            Toast.makeText(getContext(), R.string.split_failed, Toast.LENGTH_LONG).show();
                        });
                    }
                });
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

    // Inner adapter for page list
    private static class PageListAdapter extends RecyclerView.Adapter<PageListAdapter.VH> {
        interface OnPageChecked { void onChecked(int position, boolean checked); }
        private int pageCount = 0;
        private final List<Boolean> selected;
        private final OnPageChecked listener;

        PageListAdapter(List<Boolean> selected, OnPageChecked listener) {
            this.selected = selected;
            this.listener = listener;
        }

        void setPageCount(int count) { this.pageCount = count; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_split_page, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.tvFileName.setText("Page " + (position + 1));
            holder.tvPageLabel.setText("Tap checkbox to select");
            holder.checkbox.setChecked(selected.size() > position && selected.get(position));
            holder.checkbox.setOnCheckedChangeListener((btn, isChecked) -> {
                if (listener != null) listener.onChecked(position, isChecked);
            });
        }

        @Override
        public int getItemCount() { return pageCount; }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.TextView tvFileName, tvPageLabel;
            android.widget.CheckBox checkbox;
            VH(@NonNull View v) {
                super(v);
                tvFileName = v.findViewById(R.id.tv_file_name);
                tvPageLabel = v.findViewById(R.id.tv_page_label);
                checkbox = v.findViewById(R.id.checkbox_page);
            }
        }
    }
}
