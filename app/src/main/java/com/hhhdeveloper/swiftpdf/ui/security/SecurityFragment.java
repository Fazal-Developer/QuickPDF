package com.hhhdeveloper.swiftpdf.ui.security;

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
import com.hhhdeveloper.swiftpdf.databinding.FragmentSecurityBinding;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;
import com.hhhdeveloper.swiftpdf.utils.PdfSecurityUtil;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecurityFragment extends Fragment {

    private FragmentSecurityBinding binding;
    private Uri selectedPdfUri;
    private File selectedPdfFile;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isEncryptMode = true;

    private final ActivityResultLauncher<Intent> filePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    selectedPdfUri = result.getData().getData();
                    loadPdf();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSecurityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSelectPdf.setOnClickListener(v -> openFilePicker());

        binding.chipGroupOperation.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int checkedId = group.getCheckedChipId();
            if (checkedId == R.id.chip_lock) {
                isEncryptMode = true;
                binding.tvPasswordLabel.setText("Set Password");
                binding.btnExecute.setText("Protect PDF");
            } else if (checkedId == R.id.chip_unlock) {
                isEncryptMode = false;
                binding.tvPasswordLabel.setText("Enter Password to Decrypt");
                binding.btnExecute.setText("Remove Protection");
            }
        });

        binding.btnExecute.setOnClickListener(v -> executeSecurityOperation());
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
            } catch (Exception e) {
                if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to load PDF", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void executeSecurityOperation() {
        if (selectedPdfFile == null) {
            Toast.makeText(requireContext(), R.string.select_pdf, Toast.LENGTH_SHORT).show();
            return;
        }

        String password = binding.etPassword.getText().toString();
        if (password.isEmpty()) {
            Toast.makeText(requireContext(), R.string.password_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(requireContext());
        dialog.setMessage(isEncryptMode ? getString(R.string.encrypting_pdf) : getString(R.string.decrypting_pdf));
        dialog.setCancelable(false);
        dialog.show();

        if (isEncryptMode) {
            executor.submit(() -> PdfSecurityUtil.encrypt(requireContext(), selectedPdfFile, password, new PdfSecurityUtil.SecurityCallback() {
                @Override
                public void onSuccess(File outputFile) {
                    saveToRecent(outputFile, "LOCK");
                    if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                        try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                        showSuccessFeedback(outputFile, getString(R.string.security_success));
                    });
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                        try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                        Toast.makeText(getContext(), R.string.security_failed, Toast.LENGTH_LONG).show();
                    });
                }
            }));
        } else {
            executor.submit(() -> PdfSecurityUtil.decrypt(requireContext(), selectedPdfFile, password, new PdfSecurityUtil.SecurityCallback() {
                @Override
                public void onSuccess(File outputFile) {
                    saveToRecent(outputFile, "UNLOCK");
                    if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                        try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                        showSuccessFeedback(outputFile, "Protection removed successfully!");
                    });
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                        try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                        Toast.makeText(getContext(), "Invalid password or decryption failed", Toast.LENGTH_LONG).show();
                    });
                }
            }));
        }
    }

    private void showSuccessFeedback(File file, String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAction("Open Folder", v -> {
                    com.hhhdeveloper.swiftpdf.utils.FileUtil.openOutputDirectory(requireContext(), "Secured");
                })
                .show();
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
}
