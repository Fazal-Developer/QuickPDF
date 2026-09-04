package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;
import android.graphics.Color;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import com.tom_roush.pdfbox.util.Matrix;
import java.io.File;

public class PdfWatermarkUtil {

    public interface WatermarkCallback {
        void onSuccess(File outputFile);
        void onError(Exception e);
    }

    public static void addWatermark(Context context, File srcFile, String watermarkText,
                                    int textSize, float rotationDegrees, String hexColor,
                                    float opacity, WatermarkCallback callback) {
        try {
            PDDocument document = PDDocument.load(srcFile);
            PDFont font = PDType1Font.HELVETICA_BOLD;

            int parsedColor = Color.parseColor(hexColor);
            float r = Color.red(parsedColor) / 255f;
            float g = Color.green(parsedColor) / 255f;
            float b = Color.blue(parsedColor) / 255f;

            for (PDPage page : document.getPages()) {
                PDPageContentStream contentStream = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.APPEND, true, true);

                contentStream.saveGraphicsState();

                // Apply opacity transparency
                PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                graphicsState.setNonStrokingAlphaConstant(opacity);
                contentStream.setGraphicsStateParameters(graphicsState);

                // Apply font styling & colors
                contentStream.setFont(font, textSize);
                contentStream.setNonStrokingColor(r, g, b);

                float width = page.getMediaBox().getWidth();
                float height = page.getMediaBox().getHeight();

                // Compute exact string dimensions for alignment
                float textWidth = font.getStringWidth(watermarkText) / 1000f * textSize;
                float textHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000f * textSize;

                // Center position translation & angle rotation matrix
                float rad = (float) Math.toRadians(rotationDegrees);
                Matrix matrix = Matrix.getRotateInstance(rad, width / 2, height / 2);
                contentStream.setTextMatrix(matrix);

                contentStream.beginText();
                // Draw text aligned offset
                contentStream.newLineAtOffset(-textWidth / 2, -textHeight / 4);
                contentStream.showText(watermarkText);
                contentStream.endText();

                contentStream.restoreGraphicsState();
                contentStream.close();
            }

            File outputDir = FileUtil.getOutputDirectory(context);
            String outName = "watermarked_" + srcFile.getName();
            if (!outName.toLowerCase().endsWith(".pdf")) {
                outName += ".pdf";
            }
            File outFile = new File(outputDir, outName);
            document.save(outFile);
            document.close();
            callback.onSuccess(outFile);
        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
