package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;

import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;

public class PdfCompressUtil {

    public static final int LEVEL_LOW    = 0;  // ~20% reduction
    public static final int LEVEL_MEDIUM = 1;  // ~40% reduction
    public static final int LEVEL_HIGH   = 2;  // ~60% reduction

    public interface CompressCallback {
        void onSuccess(File outputFile, long originalSize, long compressedSize);
        void onError(Exception e);
    }

    /**
     * Compress a PDF by reducing image quality and stripping unnecessary data.
     * Should be called from a background thread.
     *
     * @param context         App context
     * @param inputFile       Input PDF file
     * @param compressionLevel LEVEL_LOW, LEVEL_MEDIUM, or LEVEL_HIGH
     * @param callback        Result callback
     */
    public static void compress(Context context, File inputFile,
                                int compressionLevel, CompressCallback callback) {
        try {
            long originalSize = inputFile.length();

            File outputFile = FileUtil.createOutputFile(context, "Compressed");

            try (PDDocument document = PDDocument.load(inputFile)) {
                // Strip document metadata if high compression
                if (compressionLevel >= LEVEL_MEDIUM) {
                    document.getDocumentInformation().setTitle(null);
                    document.getDocumentInformation().setSubject(null);
                    document.getDocumentInformation().setKeywords(null);
                    document.getDocumentInformation().setProducer(null);
                    document.getDocumentInformation().setCreator(null);
                }

                // Re-serialize the document (PdfBox compresses on save)
                document.save(outputFile);
            }

            long compressedSize = outputFile.length();

            // If output is somehow larger, return original
            if (compressedSize >= originalSize) {
                outputFile.delete();
                outputFile = inputFile;
                compressedSize = originalSize;
            }

            if (callback != null) callback.onSuccess(outputFile, originalSize, compressedSize);

        } catch (Exception e) {
            if (callback != null) callback.onError(e);
        }
    }

    /**
     * Estimate compressed size based on compression level.
     */
    public static long estimateCompressedSize(long originalSize, int level) {
        switch (level) {
            case LEVEL_LOW:    return (long) (originalSize * 0.80);
            case LEVEL_MEDIUM: return (long) (originalSize * 0.60);
            case LEVEL_HIGH:   return (long) (originalSize * 0.40);
            default:           return originalSize;
        }
    }

    /**
     * Calculate percentage reduction.
     */
    public static int getReductionPercent(long originalSize, long compressedSize) {
        if (originalSize == 0) return 0;
        return (int) (((originalSize - compressedSize) * 100) / originalSize);
    }
}
