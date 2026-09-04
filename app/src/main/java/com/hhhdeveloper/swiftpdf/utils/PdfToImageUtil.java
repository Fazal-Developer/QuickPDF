package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfToImageUtil {

    public static List<File> convertPdfToImages(Context context, File pdfFile) throws Exception {
        List<File> exportedImages = new ArrayList<>();
        ParcelFileDescriptor fileDescriptor = null;
        PdfRenderer pdfRenderer = null;

        try {
            fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);

            // Create target folder named after the PDF
            String cleanName = pdfFile.getName().replace(".pdf", "").replaceAll("[^a-zA-Z0-9]", "_");
            File outputDir = new File(FileUtil.getPicturesOutputDirectory(context), "Exported_" + cleanName);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            int pageCount = pdfRenderer.getPageCount();
            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = pdfRenderer.openPage(i);

                // High-resolution bitmap: 1200 x 1600 px
                int width = 1200;
                int height = 1600;
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(android.graphics.Color.WHITE);

                // Render page to bitmap
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                // Save bitmap to file
                File imageFile = new File(outputDir, "page_" + (i + 1) + ".jpg");
                FileOutputStream fos = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                fos.flush();
                fos.close();

                exportedImages.add(imageFile);
            }
        } finally {
            try {
                if (pdfRenderer != null) pdfRenderer.close();
                if (fileDescriptor != null) fileDescriptor.close();
            } catch (Exception ignored) {}
        }
        return exportedImages;
    }
}
