package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ImageToPdfUtil {

    // A4 dimensions in points (1 point = 1/72 inch)
    public static final int PAGE_WIDTH  = 595;
    public static final int PAGE_HEIGHT = 842;

    public interface ConvertCallback {
        void onSuccess(File outputFile);
        void onError(Exception e);
    }

    /**
     * Convert a list of image URIs to a single PDF file.
     * Uses Android's native PdfDocument API.
     * Should be called from a background thread.
     */
    public static void convert(Context context, List<Uri> imageUris, ConvertCallback callback) {
        PdfDocument pdfDocument = new PdfDocument();
        try {
            for (int i = 0; i < imageUris.size(); i++) {
                Uri uri = imageUris.get(i);
                Bitmap bitmap = loadBitmapFromUri(context, uri);
                if (bitmap == null) continue;

                // Scale bitmap to fit A4 page
                Bitmap scaledBitmap = scaleBitmapToPage(bitmap, PAGE_WIDTH, PAGE_HEIGHT);
                bitmap.recycle();

                // Create a new page
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        PAGE_WIDTH, PAGE_HEIGHT, i + 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                Canvas canvas = page.getCanvas();
                // Center the image on the page
                int left = (PAGE_WIDTH - scaledBitmap.getWidth()) / 2;
                int top  = (PAGE_HEIGHT - scaledBitmap.getHeight()) / 2;
                canvas.drawBitmap(scaledBitmap, left, top, null);

                pdfDocument.finishPage(page);
                scaledBitmap.recycle();
            }

            File outputFile = FileUtil.createOutputFile(context, "Images_to_PDF");
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                pdfDocument.writeTo(fos);
            }

            if (callback != null) callback.onSuccess(outputFile);

        } catch (Exception e) {
            if (callback != null) callback.onError(e);
        } finally {
            pdfDocument.close();
        }
    }

    /**
     * Load a Bitmap from a content URI.
     */
    private static Bitmap loadBitmapFromUri(Context context, Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;
            return BitmapFactory.decodeStream(inputStream);
        }
    }

    /**
     * Scale a bitmap to fit within page dimensions while preserving aspect ratio.
     */
    private static Bitmap scaleBitmapToPage(Bitmap bitmap, int pageWidth, int pageHeight) {
        int bWidth  = bitmap.getWidth();
        int bHeight = bitmap.getHeight();
        float ratio = Math.min((float) pageWidth / bWidth, (float) pageHeight / bHeight);
        // Leave a 20pt margin
        float margin = 20f;
        ratio = Math.min(ratio, Math.min((pageWidth - margin * 2) / bWidth,
                (pageHeight - margin * 2) / bHeight));
        int scaledW = Math.round(bWidth * ratio);
        int scaledH = Math.round(bHeight * ratio);
        return Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true);
    }
}
