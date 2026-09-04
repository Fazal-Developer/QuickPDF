package com.hhhdeveloper.swiftpdf.ui.imagetopdf;

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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.core.content.FileProvider;
import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;
import com.hhhdeveloper.swiftpdf.adapters.ImagePickerAdapter;
import com.hhhdeveloper.swiftpdf.database.AppDatabase;
import com.hhhdeveloper.swiftpdf.databinding.FragmentImageToPdfBinding;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.utils.ImageToPdfUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageToPdfFragment extends Fragment {

    private FragmentImageToPdfBinding binding;
    private ImagePickerAdapter adapter;
    private final List<Uri> selectedImages = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) return;
                Intent data = result.getData();
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        persistUri(uri);
                        selectedImages.add(uri);
                    }
                    adapter.notifyDataSetChanged();
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    persistUri(uri);
                    selectedImages.add(uri);
                    adapter.notifyItemInserted(selectedImages.size() - 1);
                }
                updateImageCount();
            });

    private int lastEditPosition = -1;
    private Uri cameraTempUri;
    private File cameraTempFile;

    private final ActivityResultLauncher<Intent> imageEditorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    String editedUriStr = result.getData().getStringExtra(ImageEditorActivity.EXTRA_EDITED_URI);
                    if (editedUriStr != null) {
                        Uri editedUri = Uri.parse(editedUriStr);
                        if (lastEditPosition != -1 && lastEditPosition < selectedImages.size()) {
                            selectedImages.set(lastEditPosition, editedUri);
                            adapter.notifyItemChanged(lastEditPosition);
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraTempUri != null) {
                    selectedImages.add(cameraTempUri);
                    adapter.notifyItemInserted(selectedImages.size() - 1);
                    updateImageCount();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentImageToPdfBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ImagePickerAdapter(selectedImages);
        adapter.setOnItemRemovedListener(pos -> updateImageCount());
        adapter.setOnImageEditListener((position, uri) -> {
            lastEditPosition = position;
            Intent intent = new Intent(requireContext(), ImageEditorActivity.class);
            intent.putExtra(ImageEditorActivity.EXTRA_IMAGE_URI, uri.toString());
            imageEditorLauncher.launch(intent);
        });
        binding.rvImages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvImages.setAdapter(adapter);

        // Drag-and-drop
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
                updateImageCount();
            }
        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(touchCallback);
        touchHelper.attachToRecyclerView(binding.rvImages);
        adapter.setItemTouchHelper(touchHelper);

        binding.btnAddImages.setOnClickListener(v -> showAddImagesBottomSheet());
        binding.btnConvert.setOnClickListener(v -> startConvert());
    }

    private void persistUri(Uri uri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePicker.launch(intent);
    }

    private void updateImageCount() {
        int count = selectedImages.size();
        if (count == 0) {
            binding.tvImageCount.setText("No images added yet");
        } else {
            binding.tvImageCount.setText(count + " image" + (count > 1 ? "s" : "") + " selected");
        }
    }

    private void startConvert() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(requireContext(), R.string.min_one_image, Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(requireContext());
        dialog.setMessage(getString(R.string.converting));
        dialog.setCancelable(false);
        dialog.show();

        List<Uri> urisCopy = new ArrayList<>(selectedImages);

        executor.submit(() ->
            ImageToPdfUtil.convert(requireContext(), urisCopy, new ImageToPdfUtil.ConvertCallback() {
                @Override
                public void onSuccess(File outputFile) {
                    saveToRecent(outputFile, "IMAGE_TO_PDF");
                    if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                        try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                         Snackbar.make(binding.getRoot(), getString(R.string.convert_success),
                                 Snackbar.LENGTH_LONG)
                                 .setAction("Open Folder", v -> {
                                     com.hhhdeveloper.swiftpdf.utils.FileUtil.openOutputDirectory(getContext(), "Converted");
                                 })
                                 .show();
                        selectedImages.clear();
                        adapter.notifyDataSetChanged();
                        updateImageCount();
                    });
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                        try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
                        Toast.makeText(getContext(), R.string.convert_failed, Toast.LENGTH_LONG).show();
                    });
                }
            }));
    }

    private void saveToRecent(File file, String operation) {
        RecentFile recent = new RecentFile(file.getName(), file.getAbsolutePath(),
                file.length(), System.currentTimeMillis(), operation);
        executor.submit(() -> {
            android.content.Context ctx = getContext(); if (ctx == null) return; AppDatabase.getInstance(ctx.getApplicationContext()).recentFileDao().insert(recent);
            FileUtil.scanSavedFile(requireContext(), file);
        });
    }

    private void showAddImagesBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_add_images_sheet, null);

        view.findViewById(R.id.layout_camera).setOnClickListener(v -> {
            try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
            try {
                cameraTempFile = new File(requireContext().getCacheDir(), "scan_" + System.currentTimeMillis() + ".jpg");
                cameraTempUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider", cameraTempFile);
                cameraLauncher.launch(cameraTempUri);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to prepare Camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.layout_gallery).setOnClickListener(v -> {
            try { if (dialog != null) dialog.dismiss(); } catch (Exception ignored) {}
            openImagePicker();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
