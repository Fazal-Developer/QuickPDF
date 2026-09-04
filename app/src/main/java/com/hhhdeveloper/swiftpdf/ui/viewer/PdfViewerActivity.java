package com.hhhdeveloper.swiftpdf.ui.viewer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.databinding.ActivityPdfViewerBinding;
import com.hhhdeveloper.swiftpdf.ui.editor.EditPdfActivity;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfViewerActivity extends AppCompatActivity
        implements OnPageChangeListener, OnLoadCompleteListener, OnErrorListener {

    public static final String EXTRA_PDF_URI  = "pdf_uri";
    public static final String EXTRA_PDF_PATH = "pdf_path";
    public static final String EXTRA_OPEN_PICKER = "open_picker";

    private ActivityPdfViewerBinding binding;
    private int totalPages = 0;
    private int currentPage = 0;

    private Uri loadedUri = null;
    private File loadedFile = null;
    private final List<Integer> searchMatches = new ArrayList<>();
    private int currentMatchIndex = -1;
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();

    private final androidx.activity.result.ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts
                    .StartActivityForResult(), result -> {
                if (result.getData() != null && result.getData().getData() != null) {
                    openPdfUri(result.getData().getData());
                } else {
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("PDF Document");
        }

        // Bind Search Overlay elements
        binding.btnCloseSearch.setOnClickListener(v -> toggleSearch(false));

        binding.etPdfSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.etPdfSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        binding.btnNextMatch.setOnClickListener(v -> navigateMatch(true));
        binding.btnPrevMatch.setOnClickListener(v -> navigateMatch(false));

        // Floating Page Controls
        binding.btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 0) binding.pdfView.jumpTo(currentPage - 1);
        });

        binding.btnNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) binding.pdfView.jumpTo(currentPage + 1);
        });

        binding.btnFirstPage.setOnClickListener(v -> {
            if (totalPages > 0) binding.pdfView.jumpTo(0);
        });

        binding.btnLastPage.setOnClickListener(v -> {
            if (totalPages > 0) binding.pdfView.jumpTo(totalPages - 1);
        });

        // Bottom Bar Actions
        binding.btnBottomSearch.setOnClickListener(v -> toggleSearch(true));
        binding.btnBottomShare.setOnClickListener(v -> shareCurrentPdf());
        binding.btnBottomThumbnail.setOnClickListener(v -> showDocumentDetails());
        binding.btnBottomMore.setOnClickListener(v -> showViewerOptionsSheet());

        String uriString  = getIntent().getStringExtra(EXTRA_PDF_URI);
        String pathString = getIntent().getStringExtra(EXTRA_PDF_PATH);

        if (uriString != null) {
            openPdfUri(Uri.parse(uriString));
        } else if (pathString != null) {
            openPdfFile(new File(pathString));
        } else {
            openFilePicker();
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePicker.launch(intent);
    }

    private void openPdfUri(Uri uri) {
        this.loadedUri = uri;
        this.loadedFile = null;

        String name = FileUtil.getFileName(this, uri);
        if (getSupportActionBar() != null && name != null) {
            getSupportActionBar().setTitle(name);
        }

        binding.pdfView.fromUri(uri)
                .defaultPage(0)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .enableAnnotationRendering(false)
                .onLoad(this)
                .onPageChange(this)
                .onError(this)
                .scrollHandle(null) // Disabled default scrollbox handle in favor of clean floating pill
                .spacing(12)
                .autoSpacing(true)
                .pageSnap(true)
                .pageFitPolicy(FitPolicy.WIDTH)
                .load();
    }

    private void openPdfFile(File file) {
        if (!file.exists()) {
            Toast.makeText(this, getString(R.string.error_loading_pdf), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        this.loadedFile = file;
        this.loadedUri = null;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(file.getName());
        }

        binding.pdfView.fromFile(file)
                .defaultPage(0)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .enableAnnotationRendering(false)
                .onLoad(this)
                .onPageChange(this)
                .onError(this)
                .scrollHandle(null) // Disabled default scrollbox handle in favor of clean floating pill
                .spacing(12)
                .autoSpacing(true)
                .pageSnap(true)
                .pageFitPolicy(FitPolicy.WIDTH)
                .load();
    }

    private void showViewerOptionsSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_viewer_options_sheet, null);

        TextView tvTitle = view.findViewById(R.id.tv_viewer_sheet_title);
        if (loadedFile != null) {
            tvTitle.setText(loadedFile.getName());
        } else if (loadedUri != null) {
            String name = FileUtil.getFileName(this, loadedUri);
            tvTitle.setText(name != null ? name : "PDF Document");
        }

        // Print Action
        view.findViewById(R.id.layout_viewer_print).setOnClickListener(v -> {
            dialog.dismiss();
            printCurrentPdf();
        });

        // Edit Action
        view.findViewById(R.id.layout_viewer_edit).setOnClickListener(v -> {
            dialog.dismiss();
            if (loadedFile != null) {
                Intent intent = new Intent(this, EditPdfActivity.class);
                intent.putExtra(EditPdfActivity.EXTRA_PDF_PATH, loadedFile.getAbsolutePath());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Editing requires a saved PDF file", Toast.LENGTH_SHORT).show();
            }
        });

        // Share Action
        view.findViewById(R.id.layout_viewer_share).setOnClickListener(v -> {
            dialog.dismiss();
            shareCurrentPdf();
        });

        // Details Action
        view.findViewById(R.id.layout_viewer_info).setOnClickListener(v -> {
            dialog.dismiss();
            showDocumentDetails();
        });

        // Delete Action
        view.findViewById(R.id.layout_viewer_delete).setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeleteDocument();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void printCurrentPdf() {
        if (loadedFile != null && loadedFile.exists()) {
            try {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                if (printManager != null) {
                    String jobName = getString(R.string.app_name) + " - " + loadedFile.getName();
                    printManager.print(jobName, new PrintDocumentAdapter() {
                        @Override
                        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                             CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
                            if (cancellationSignal.isCanceled()) {
                                callback.onLayoutCancelled();
                                return;
                            }
                            PrintDocumentInfo info = new PrintDocumentInfo.Builder(loadedFile.getName())
                                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                    .setPageCount(totalPages)
                                    .build();
                            callback.onLayoutFinished(info, true);
                        }

                        @Override
                        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                            CancellationSignal cancellationSignal, WriteResultCallback callback) {
                            try (InputStream input = new FileInputStream(loadedFile);
                                 OutputStream output = new FileOutputStream(destination.getFileDescriptor())) {
                                byte[] buf = new byte[8192];
                                int bytesRead;
                                while ((bytesRead = input.read(buf)) > 0) {
                                    output.write(buf, 0, bytesRead);
                                }
                                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                            } catch (Exception e) {
                                callback.onWriteFailed(e.getMessage());
                            }
                        }
                    }, null);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to start print job", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Printing requires local file access", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteDocument() {
        if (loadedFile == null || !loadedFile.exists()) {
            Toast.makeText(this, "File cannot be deleted directly", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete " + loadedFile.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FileUtil.deleteFile(loadedFile.getAbsolutePath());
                    Toast.makeText(this, "Document deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDocumentDetails() {
        StringBuilder details = new StringBuilder();
        if (loadedFile != null) {
            details.append("Name: ").append(loadedFile.getName()).append("\n\n");
            details.append("Size: ").append(FileUtil.formatFileSize(loadedFile.length())).append("\n\n");
            details.append("Total Pages: ").append(totalPages).append("\n\n");
            details.append("Location:\n").append(loadedFile.getAbsolutePath());
        } else if (loadedUri != null) {
            String name = FileUtil.getFileName(this, loadedUri);
            details.append("Name: ").append(name != null ? name : "Unknown").append("\n\n");
            details.append("Total Pages: ").append(totalPages).append("\n\n");
            details.append("URI: ").append(loadedUri.toString());
        }

        new AlertDialog.Builder(this)
                .setTitle("Document Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void toggleSearch(boolean show) {
        if (show) {
            binding.cardSearchBar.setVisibility(View.VISIBLE);
            binding.etPdfSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.etPdfSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        } else {
            binding.cardSearchBar.setVisibility(View.GONE);
            searchMatches.clear();
            currentMatchIndex = -1;
            hideKeyboard();
        }
    }

    private void shareCurrentPdf() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");

            if (loadedFile != null) {
                shareIntent.putExtra(Intent.EXTRA_STREAM, FileUtil.getShareUri(this, loadedFile));
            } else if (loadedUri != null) {
                shareIntent.putExtra(Intent.EXTRA_STREAM, loadedUri);
            } else {
                Toast.makeText(this, "No document loaded to share", Toast.LENGTH_SHORT).show();
                return;
            }

            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share document", Toast.LENGTH_SHORT).show();
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) return;
        binding.tvMatchCount.setText("...");
        hideKeyboard();

        searchExecutor.submit(() -> {
            try {
                com.tom_roush.pdfbox.pdmodel.PDDocument document = null;
                if (loadedFile != null) {
                    document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(loadedFile);
                } else if (loadedUri != null) {
                    InputStream is = getContentResolver().openInputStream(loadedUri);
                    if (is != null) {
                        document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(is);
                    }
                }

                if (document == null) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(this, "Failed to parse document text", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                com.tom_roush.pdfbox.text.PDFTextStripper stripper = new com.tom_roush.pdfbox.text.PDFTextStripper();
                int pageCount = document.getNumberOfPages();
                final List<Integer> tempMatches = new ArrayList<>();
                for (int i = 0; i < pageCount; i++) {
                    stripper.setStartPage(i + 1);
                    stripper.setEndPage(i + 1);
                    String pageText = stripper.getText(document);
                    if (pageText != null && pageText.toLowerCase().contains(query.toLowerCase())) {
                        tempMatches.add(i);
                    }
                }
                document.close();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    searchMatches.clear();
                    searchMatches.addAll(tempMatches);
                    if (searchMatches.isEmpty()) {
                        currentMatchIndex = -1;
                        binding.tvMatchCount.setText("0/0");
                        Toast.makeText(this, "No matches found", Toast.LENGTH_SHORT).show();
                    } else {
                        currentMatchIndex = 0;
                        int targetPage = searchMatches.get(0);
                        binding.pdfView.jumpTo(targetPage);
                        binding.tvMatchCount.setText("1/" + searchMatches.size());
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Error searching document text", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void navigateMatch(boolean next) {
        if (searchMatches.isEmpty()) return;
        if (next) {
            currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size();
        } else {
            currentMatchIndex = (currentMatchIndex - 1 + searchMatches.size()) % searchMatches.size();
        }
        int targetPage = searchMatches.get(currentMatchIndex);
        binding.pdfView.jumpTo(targetPage);
        binding.tvMatchCount.setText((currentMatchIndex + 1) + "/" + searchMatches.size());
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && binding.etPdfSearch != null) {
            imm.hideSoftInputFromWindow(binding.etPdfSearch.getWindowToken(), 0);
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        this.currentPage = page;
        this.totalPages = pageCount;
        binding.tvPageInfo.setText((page + 1) + " / " + pageCount);
    }

    @Override
    public void loadComplete(int nbPages) {
        this.totalPages = nbPages;
        binding.tvPageInfo.setText("1 / " + nbPages);
    }

    @Override
    public void onError(Throwable t) {
        Toast.makeText(this, getString(R.string.error_loading_pdf), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_viewer, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        if (searchExecutor != null && !searchExecutor.isShutdown()) {
            searchExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            boolean isVisible = binding.cardSearchBar.getVisibility() == View.VISIBLE;
            toggleSearch(!isVisible);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
