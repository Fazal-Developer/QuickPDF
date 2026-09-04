package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class WordToPdfUtil {

    public static List<String> extractTextFromDocx(File docxFile) {
        List<String> paragraphs = new ArrayList<>();
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(docxFile);
            ZipEntry entry = zipFile.getEntry("word/document.xml");
            if (entry != null) {
                InputStream is = zipFile.getInputStream(entry);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(is, "UTF-8");
                int eventType = parser.getEventType();
                StringBuilder paragraphBuilder = new StringBuilder();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String name = parser.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        if ("w:t".equalsIgnoreCase(name) || "t".equalsIgnoreCase(name)) {
                            // Extract plain text runs
                            parser.next();
                            if (parser.getText() != null) {
                                paragraphBuilder.append(parser.getText());
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if ("w:p".equalsIgnoreCase(name) || "p".equalsIgnoreCase(name)) {
                            // Paragraph close tag, write to list
                            String clean = paragraphBuilder.toString().trim();
                            if (!clean.isEmpty()) {
                                paragraphs.add(clean);
                            }
                            paragraphBuilder.setLength(0);
                        }
                    }
                    eventType = parser.next();
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (zipFile != null) {
                try { zipFile.close(); } catch (Exception ignored) {}
            }
        }
        return paragraphs;
    }

    public static List<String> extractTextFromTxt(File txtFile) {
        List<String> paragraphs = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(txtFile));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    paragraphs.add(line);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) {}
            }
        }
        return paragraphs;
    }

    public static void convertTextToPdf(List<String> paragraphs, File pdfFile) throws Exception {
        PdfDocument document = new PdfDocument();

        int pageWidth = 612; // Letter width (8.5 in x 72 pt/in)
        int pageHeight = 792; // Letter height (11 in x 72 pt/in)
        int margin = 54; // 0.75 in margin
        int contentWidth = pageWidth - (margin * 2);

        Paint paint = new Paint();
        paint.setTextSize(11f);
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);

        Paint headerPaint = new Paint(paint);
        headerPaint.setTextSize(18f);
        headerPaint.setFakeBoldText(true);

        int pageNum = 1;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        float y = margin + 30;

        // Draw title header
        canvas.drawText("SwiftPDF Exported Document", margin, y, headerPaint);
        y += 40;

        for (String para : paragraphs) {
            List<String> lines = wrapText(para, paint, contentWidth);
            for (String line : lines) {
                if (y + 20 > pageHeight - margin) {
                    // Start next page
                    document.finishPage(page);
                    pageNum++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = margin + 20;
                }
                canvas.drawText(line, margin, y, paint);
                y += 16;
            }
            y += 10; // Space between paragraphs
        }

        document.finishPage(page);
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
