package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PptxToPdfUtil {

    public static void convertPptxToPdf(File pptxFile, File pdfFile) throws Exception {
        ZipFile zipFile = null;
        List<List<String>> allSlidesText = new ArrayList<>();

        try {
            zipFile = new ZipFile(pptxFile);

            // Iterate ppt/slides/slide1.xml, slide2.xml, etc. until we hit a missing index
            int slideNum = 1;
            while (true) {
                ZipEntry slideEntry = zipFile.getEntry("ppt/slides/slide" + slideNum + ".xml");
                if (slideEntry == null) {
                    break; // No more slides
                }

                InputStream is = zipFile.getInputStream(slideEntry);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(is, "UTF-8");

                List<String> slideParagraphs = new ArrayList<>();
                StringBuilder runText = new StringBuilder();
                int eventType = parser.getEventType();

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String name = parser.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        if ("a:t".equals(name) || "t".equals(name)) {
                            parser.next();
                            if (parser.getText() != null) {
                                runText.append(parser.getText());
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if ("a:p".equals(name) || "p".equals(name)) {
                            String para = runText.toString().trim();
                            if (!para.isEmpty()) {
                                slideParagraphs.add(para);
                            }
                            runText.setLength(0);
                        }
                    }
                    eventType = parser.next();
                }
                is.close();

                if (!slideParagraphs.isEmpty()) {
                    allSlidesText.add(slideParagraphs);
                } else {
                    // Empty slide placeholder
                    List<String> emptySlide = new ArrayList<>();
                    emptySlide.add("[Slide " + slideNum + " - Empty or Image Slide]");
                    allSlidesText.add(emptySlide);
                }

                slideNum++;
            }

        } finally {
            if (zipFile != null) {
                try { zipFile.close(); } catch (Exception ignored) {}
            }
        }

        if (allSlidesText.isEmpty()) {
            throw new Exception("PowerPoint document contains no slides.");
        }

        // 3. Render slide text blocks onto landscape PDF pages
        PdfDocument document = new PdfDocument();

        int pageWidth = 792; // Slide standard: landscape Letter layout
        int pageHeight = 612;
        int margin = 54;
        int contentWidth = pageWidth - (margin * 2);

        Paint textPaint = new Paint();
        textPaint.setTextSize(12f);
        textPaint.setColor(Color.parseColor("#333333"));
        textPaint.setAntiAlias(true);

        Paint titlePaint = new Paint();
        titlePaint.setTextSize(22f);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(Color.parseColor("#1A237E")); // Premium Deep Blue for PPT title headers
        titlePaint.setAntiAlias(true);

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#E0E0E0"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        int slideIdx = 1;
        for (List<String> slideText : allSlidesText) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, slideIdx).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Draw landscape slide border
            canvas.drawRect(20, 20, pageWidth - 20, pageHeight - 20, borderPaint);

            float y = margin + 30;

            // Draw Slide Title (first line of text)
            String slideTitle = "Slide " + slideIdx;
            if (!slideText.isEmpty()) {
                slideTitle = slideText.get(0);
            }
            canvas.drawText(slideTitle, margin, y, titlePaint);
            y += 40;

            // Draw Subtitle / Bullet list items
            for (int i = 1; i < slideText.size(); i++) {
                String bullet = slideText.get(i);
                List<String> lines = wrapText("•  " + bullet, textPaint, contentWidth);

                for (String line : lines) {
                    if (y + 20 > pageHeight - margin) {
                        break; // slide viewport overflow protection
                    }
                    canvas.drawText(line, margin + 12, y, textPaint);
                    y += 20;
                }
                y += 8;
            }

            document.finishPage(page);
            slideIdx++;
        }

        FileOutputStream fos = new FileOutputStream(pdfFile);
        document.writeTo(fos);
        fos.flush();
        fos.close();
        document.close();
    }

    private static List<String> wrapText(String text, Paint paint, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String testLine = line.toString() + (line.length() == 0 ? "" : " ") + word;
            float width = paint.measureText(testLine);
            if (width <= maxWidth) {
                line.append(line.length() == 0 ? "" : " ").append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
