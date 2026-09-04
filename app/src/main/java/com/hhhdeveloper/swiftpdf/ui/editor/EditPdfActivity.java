package com.hhhdeveloper.swiftpdf.ui.editor;

import android.content.Intent;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.adapters.PdfPageAdapter;
import com.hhhdeveloper.swiftpdf.database.AppDatabase;
import com.hhhdeveloper.swiftpdf.databinding.ActivityEditPdfBinding;
import com.hhhdeveloper.swiftpdf.models.PageItem;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;
import com.hhhdeveloper.swiftpdf.utils.PdfRendererCache;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditPdfActivity extends AppCompatActivity {

    public static final String EXTRA_PDF_PATH = "extra_pdf_path";

    private ActivityEditPdfBinding binding;
    private File pdfFile;
    private final PdfRendererCache rendererCache = new PdfRendererCache();
    private PdfPageAdapter adapter;
    private final List<PageItem> pageItems = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> addPdfPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri uri = result.getData().getData();
                    executor.submit(() -> {
                        try {
                            File addedFile = FileUtil.copyUriToTempFile(this, uri);
                            
                            // Determine pages count of the added file
                            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(addedFile, ParcelFileDescriptor.MODE_READ_ONLY);
                            PdfRenderer renderer = new PdfRenderer(pfd);
                            int pagesCount = renderer.getPageCount();
                            renderer.close();
                            pfd.close();

                            runOnUiThread(() -> {
                                for (int i = 0; i < pagesCount; i++) {
                                    pageItems.add(new PageItem(addedFile, i));
                                }
                                adapter.notifyDataSetChanged();
                                Toast.makeText(this, "Added " + pagesCount + " pages to editor", Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, "Failed to load pages: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditPdfBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        PDFBoxResourceLoader.init(getApplicationContext());

        if (getIntent() == null || !getIntent().hasExtra(EXTRA_PDF_PATH)) {
            Toast.makeText(this, "No PDF path provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String path = getIntent().getStringExtra(EXTRA_PDF_PATH);
        pdfFile = new File(path);

        if (!pdfFile.exists()) {
            Toast.makeText(this, "File does not exist", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPdfPages();

        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveModifiedPdf());
        binding.fabAddPages.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            addPdfPickerLauncher.launch(intent);
        });
    }

    private void loadPdfPages() {
        try {
            // Read initial pages count from original file
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(pfd);
            int pageCount = renderer.getPageCount();
            renderer.close();
            pfd.close();

            for (int i = 0; i < pageCount; i++) {
                pageItems.add(new PageItem(pdfFile, i));
            }

            adapter = new PdfPageAdapter(pageItems, rendererCache, position -> {
                if (pageItems.size() <= 1) {
                    Toast.makeText(this, "Cannot delete the only page in document", Toast.LENGTH_SHORT).show();
                    return;
                }
                pageItems.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, pageItems.size());
            });

            binding.rvPages.setLayoutManager(new GridLayoutManager(this, 2));
            binding.rvPages.setAdapter(adapter);

            // Drag to reorder
            ItemTouchHelper.SimpleCallback dragCallback = new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView,
                                       @NonNull RecyclerView.ViewHolder viewHolder,
                                       @NonNull RecyclerView.ViewHolder target) {
                    return adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
            };

            ItemTouchHelper helper = new ItemTouchHelper(dragCallback);
            helper.attachToRecyclerView(binding.rvPages);

        } catch (Exception e) {
            Toast.makeText(this, "Error reading PDF pages: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void saveModifiedPdf() {
        if (pageItems.isEmpty()) {
            Toast.makeText(this, "Document cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSave.setEnabled(false);

        executor.submit(() -> {
            PDDocument outDoc = null;
            Map<File, PDDocument> loadedDocs = new HashMap<>();
            try {
                // Close active renderers before compiling output to avoid file access locking
                rendererCache.closeAll();

                outDoc = new PDDocument();

                // Re-compile pages based on the sorted pageItems list
                for (PageItem item : pageItems) {
                    PDDocument doc = loadedDocs.get(item.sourceFile);
                    if (doc == null) {
                        doc = PDDocument.load(item.sourceFile);
                        loadedDocs.put(item.sourceFile, doc);
                    }
                    outDoc.addPage(doc.getPage(item.originalPageIndex));
                }

                // Save to temporary file and overwrite original
                File tempFile = new File(getCacheDir(), "edit_" + System.currentTimeMillis() + ".pdf");
                outDoc.save(tempFile);

                // Overwrite original
                if (tempFile.renameTo(pdfFile)) {
                    // Success
                    RecentFile recent = new RecentFile(pdfFile.getName(), pdfFile.getAbsolutePath(),
                            pdfFile.length(), System.currentTimeMillis(), "EDIT");
                    AppDatabase.getInstance(this).recentFileDao().insert(recent);
                    com.hhhdeveloper.swiftpdf.utils.FileUtil.scanSavedFile(this, pdfFile);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "PDF updated successfully!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                } else {
                    throw new Exception("Unable to overwrite original file.");
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to save edits: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    binding.btnSave.setEnabled(true);
                });
            } finally {
                if (outDoc != null) {
                    try { outDoc.close(); } catch (Exception ignored) {}
                }
                for (PDDocument doc : loadedDocs.values()) {
                    try { doc.close(); } catch (Exception ignored) {}
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rendererCache.closeAll();
        executor.shutdown();
    }
}
