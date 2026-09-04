package com.hhhdeveloper.swiftpdf.ui.recent;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.hhhdeveloper.swiftpdf.ui.editor.EditPdfActivity;
import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.adapters.RecentFilesAdapter;
import com.hhhdeveloper.swiftpdf.database.AppDatabase;
import com.hhhdeveloper.swiftpdf.databinding.FragmentRecentFilesBinding;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.ui.viewer.PdfViewerActivity;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecentFilesFragment extends Fragment implements RecentFilesAdapter.OnFileActionListener {

    private FragmentRecentFilesBinding binding;
    private RecentFilesAdapter adapter;
    private final List<RecentFile> fileList = new ArrayList<>();
    private final List<RecentFile> masterFileList = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AppDatabase db;

    private final ActivityResultLauncher<Intent> editPdfLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {});

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRecentFilesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getInstance(requireContext().getApplicationContext());

        adapter = new RecentFilesAdapter(fileList, true);
        adapter.setOnFileActionListener(this);
        binding.rvRecentFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentFiles.setAdapter(adapter);

        // Setup Search Listener
        binding.etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                filterFiles();
            }
        });

        // Setup Filter Chips Listener
        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> filterFiles());

        // Observe LiveData from Room
        db.recentFileDao().getAllFiles().observe(getViewLifecycleOwner(), files -> {
            masterFileList.clear();
            if (files != null) masterFileList.addAll(files);
            filterFiles();
        });
    }

    private void filterFiles() {
        if (binding == null) return;
        String query = binding.etSearch.getText().toString().trim().toLowerCase();
        int checkedChipId = binding.chipGroupFilters.getCheckedChipId();

        String targetOp = null;
        if (checkedChipId == R.id.chip_merge)         targetOp = "MERGE";
        else if (checkedChipId == R.id.chip_split)    targetOp = "SPLIT";
        else if (checkedChipId == R.id.chip_compress) targetOp = "COMPRESS";
        else if (checkedChipId == R.id.chip_image_to_pdf) targetOp = "IMAGE_TO_PDF";

        fileList.clear();
        for (RecentFile file : masterFileList) {
            boolean matchesQuery = file.getFileName().toLowerCase().contains(query);
            boolean matchesOp = (targetOp == null || targetOp.equals(file.getOperation()));
            if (matchesQuery && matchesOp) {
                fileList.add(file);
            }
        }
        adapter.setFiles(fileList, true);

        if (fileList.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvRecentFiles.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvRecentFiles.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_recent, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_all) {
            showClearAllDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.clear_all))
                .setMessage(getString(R.string.clear_all_confirm))
                .setPositiveButton(getString(R.string.ok), (d, w) ->
                        executor.submit(() -> db.recentFileDao().deleteAll()))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    @Override
    public void onOpen(RecentFile file) {
        File f = new File(file.getFilePath());
        if (!f.exists()) {
            Toast.makeText(requireContext(), "File no longer exists", Toast.LENGTH_SHORT).show();
            return;
        }
        if (file.getFilePath().endsWith(".pdf")) {
            Intent intent = new Intent(requireContext(), PdfViewerActivity.class);
            intent.putExtra(PdfViewerActivity.EXTRA_PDF_PATH, file.getFilePath());
            startActivity(intent);
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider", f),
                        file.getFilePath().endsWith(".txt") ? "text/plain" : "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "No application found to view this file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onShare(RecentFile file) {
        File f = new File(file.getFilePath());
        if (!f.exists()) {
            Toast.makeText(requireContext(), "File no longer exists", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, FileUtil.getShareUri(requireContext(), f));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share PDF via"));
    }

    @Override
    public void onRename(RecentFile file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.rename_file));
        final EditText input = new EditText(requireContext());
        input.setText(file.getFileName().replace(".pdf", ""));
        input.selectAll();
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.ok), (d, w) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) return;
            if (FileUtil.renameFile(file.getFilePath(), newName)) {
                // Update DB
                String newFileName = newName.endsWith(".pdf") ? newName : newName + ".pdf";
                String newPath = new File(file.getFilePath()).getParent() + "/" + newFileName;
                file.setFileName(newFileName);
                file.setFilePath(newPath);
                executor.submit(() -> db.recentFileDao().update(file));
            } else {
                Toast.makeText(requireContext(), "Rename failed", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    @Override
    public void onDelete(RecentFile file) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_confirm))
                .setMessage(getString(R.string.delete_confirm_msg))
                .setPositiveButton(getString(R.string.ok), (d, w) -> {
                    FileUtil.deleteFile(file.getFilePath());
                    executor.submit(() -> db.recentFileDao().delete(file));
                    Toast.makeText(requireContext(), R.string.file_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    @Override
    public void onEditPages(RecentFile file) {
        Intent intent = new Intent(requireContext(), EditPdfActivity.class);
        intent.putExtra(EditPdfActivity.EXTRA_PDF_PATH, file.getFilePath());
        editPdfLauncher.launch(intent);
    }

    @Override
    public void onLocate(RecentFile file) {
        File f = new File(file.getFilePath());
        if (!f.exists()) {
            Toast.makeText(requireContext(), "File no longer exists", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File parentDir = f.getParentFile();
            if (parentDir != null) {
                String subfolderName = parentDir.getName();
                // If parent name is not one of our standard subfolders, fallback to opening root directory
                if (subfolderName.equalsIgnoreCase("SwiftPDF")) {
                    subfolderName = null;
                }
                FileUtil.openOutputDirectory(requireContext(), subfolderName);
            } else {
                FileUtil.openOutputDirectory(requireContext(), null);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to locate file directory", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
