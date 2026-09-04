package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;

import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class PdfMergeUtil {

    public interface MergeCallback {
        void onSuccess(File outputFile);
        void onError(Exception e);
    }

    /**
     * Merges a list of PDF files into a single output file.
     * Should be called from a background thread.
     *
     * @param context  App context (for output directory)
     * @param pdfFiles List of input PDF files to merge
     * @param callback Result callback
     */
    public static void merge(Context context, List<File> pdfFiles, MergeCallback callback) {
        try {
            if (pdfFiles == null || pdfFiles.size() < 2) {
                throw new IllegalArgumentException("Need at least 2 files to merge");
            }

            File outputFile = FileUtil.createOutputFile(context, "Merged");
            PDFMergerUtility merger = new PDFMergerUtility();
            merger.setDestinationFileName(outputFile.getAbsolutePath());

            for (File pdf : pdfFiles) {
                if (!pdf.exists()) {
                    throw new IllegalArgumentException("File not found: " + pdf.getName());
                }
                merger.addSource(new FileInputStream(pdf));
            }

            merger.mergeDocuments(null);

            if (callback != null) callback.onSuccess(outputFile);

        } catch (Exception e) {
            if (callback != null) callback.onError(e);
        }
    }

    /**
     * Gets the total page count of a PDF file.
     */
    public static int getPageCount(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            return document.getNumberOfPages();
        } catch (Exception e) {
            return 0;
        }
    }
}
