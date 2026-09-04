package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PdfSplitUtil {

    public interface SplitCallback {
        void onSuccess(List<File> outputFiles);
        void onError(Exception e);
    }

    /**
     * Split a PDF by extracting selected page indices into separate files.
     * Page indices are 0-based.
     * Should be called from a background thread.
     */
    public static void splitByPages(Context context, File inputFile,
                                    List<Integer> pageIndices, SplitCallback callback) {
        try {
            List<File> outputFiles = new ArrayList<>();

            try (PDDocument sourceDoc = PDDocument.load(inputFile)) {
                int totalPages = sourceDoc.getNumberOfPages();

                for (int pageIndex : pageIndices) {
                    if (pageIndex < 0 || pageIndex >= totalPages) continue;

                    PDDocument singlePage = new PDDocument();
                    PDPage page = sourceDoc.getPage(pageIndex);
                    singlePage.addPage(page);

                    File outputFile = FileUtil.createOutputFile(context,
                            "Split_Page" + (pageIndex + 1));
                    singlePage.save(outputFile);
                    singlePage.close();

                    outputFiles.add(outputFile);
                }
            }

            if (callback != null) callback.onSuccess(outputFiles);

        } catch (Exception e) {
            if (callback != null) callback.onError(e);
        }
    }

    /**
     * Split a PDF into ranges, e.g. "1-3", "4-6".
     * Should be called from a background thread.
     */
    public static void splitByRange(Context context, File inputFile,
                                    int fromPage, int toPage, SplitCallback callback) {
        List<Integer> pages = new ArrayList<>();
        for (int i = fromPage - 1; i < toPage; i++) {
            pages.add(i);
        }
        splitByPages(context, inputFile, pages, callback);
    }

    /**
     * Get total page count of a PDF.
     */
    public static int getPageCount(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            return document.getNumberOfPages();
        } catch (Exception e) {
            return 0;
        }
    }
}
