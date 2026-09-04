package com.hhhdeveloper.swiftpdf.ui.merge;

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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.adapters.MergeFileAdapter;
import com.hhhdeveloper.swiftpdf.database.AppDatabase;
import com.hhhdeveloper.swiftpdf.databinding.FragmentMergeBinding;
import com.hhhdeveloper.swiftpdf.models.PdfFile;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;
import com.hhhdeveloper.swiftpdf.utils.PdfMergeUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MergeFragment extends Fragment {

    private FragmentMergeBinding binding;
    private MergeFileAdapter adapter;
    private final List<PdfFile> selectedFiles = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) return;
                Intent data = result.getData();

                // Handle multiple selection
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        addFileFromUri(uri);
                    }
                } else if (data.getData() != null) {
                    addFileFromUri(data.getData());
                }
                updateFileCount();
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMergeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup RecyclerView
        adapter = new MergeFileAdapter(selectedFiles);
        adapter.setOnItemRemovedListener(pos -> updateFileCount());
        binding.rvFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFiles.setAdapter(adapter);

        // ItemTouchHelper for drag-and-drop
        ItemTouchHelper.SimpleCallback touchCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder from,
                                  @NonNull RecyclerView.ViewHolder to) {
                return adapter.onMove(rv, from, to);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                adapter.onSwiped(viewHolder, direction);
                updateFileCount();
            }
        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(touchCallback);
        touchHelper.attachToRecyclerView(binding.rvFiles);
        adapter.setItemTouchHelper(touchHelper);

        binding.btnAddFiles.setOnClickListener(v -> openFilePicker());
        binding.btnMerge.setOnClickListener(v -> startMerge());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePicker.launch(intent);
    }

    private void addFileFromUri(Uri uri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}

        String name = FileUtil.getFileName(requireContext(), uri);
        long size = FileUtil.getFileSize(requireContext(), uri);
        selectedFiles.add(new PdfFile(name, null, uri.toString(), size));
        adapter.notifyItemInserted(selectedFiles.size() - 1);
    }

    private void updateFileCount() {
        int count = selectedFiles.size();
        if (count == 0) {
            binding.tvFileCount.setText("No files added yet");
        } else {
            binding.tvFileCount.setText(count + " file" + (count > 1 ? "s" : "") + " selected");
        }
    }

    private void startMerge() {
        if (selectedFiles.size() < 2) {
            Toast.makeText(requireContext(), R.string.min_two_files, Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getString(R.string.merging_pdfs));
        progressDialog.setCancelable(false);
        progressDialog.show();

        executor.submit(() -> {
            // Copy URIs to temp files
            List<File> tempFiles = new ArrayList<>();
            try {
                for (PdfFile pf : selectedFiles) {
                    File temp = FileUtil.copyUriToTempFile(requireContext(),
                            Uri.parse(pf.getUri()));
                    tempFiles.add(temp);
                }
            } catch (Exception e) {
                if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                    try { if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss(); } catch (Exception ignored) {}
                    Toast.makeText(getContext(), R.string.merge_failed, Toast.LENGTH_LONG).show();
                });
                return;
            }

            PdfMergeUtil.merge(requireContext(), tempFiles, new PdfMergeUtil.MergeCallback() {
                @Override
                public void onSuccess(File outputFile) {
                    // Save to recent files DB
                    saveToRecent(outputFile, "MERGE");

                    if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                        try { if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss(); } catch (Exception ignored) {}
                        showSuccessSnackbar(outputFile);
                        // Clear selected files
                        selectedFiles.clear();
                        adapter.notifyDataSetChanged();
                        updateFileCount();
                    });
                    // Clean temp files
                    for (File f : tempFiles) f.delete();
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                        try { if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss(); } catch (Exception ignored) {}
                        Toast.makeText(getContext(), R.string.merge_failed, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void saveToRecent(File file, String operation) {
        RecentFile recent = new RecentFile(
                file.getName(), file.getAbsolutePath(), file.length(),
                System.currentTimeMillis(), operation);
        executor.submit(() -> {
            android.content.Context ctx = getContext(); if (ctx == null) return; AppDatabase.getInstance(ctx.getApplicationContext()).recentFileDao().insert(recent);
            FileUtil.scanSavedFile(requireContext(), file);
        });
    }

    private void showSuccessSnackbar(File outputFile) {
        Snackbar.make(binding.getRoot(), getString(R.string.merge_success), Snackbar.LENGTH_LONG)
                .setAction("Open Folder", v -> {
                    com.hhhdeveloper.swiftpdf.utils.FileUtil.openOutputDirectory(requireContext(), "Merged");
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
