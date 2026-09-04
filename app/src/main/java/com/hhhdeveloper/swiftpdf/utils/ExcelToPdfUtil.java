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

public class ExcelToPdfUtil {

    public static void convertExcelToPdf(File xlsxFile, File pdfFile) throws Exception {
        List<String> sharedStrings = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(xlsxFile);

            // 1. Parse Shared Strings
            ZipEntry sstEntry = zipFile.getEntry("xl/sharedStrings.xml");
            if (sstEntry != null) {
                InputStream is = zipFile.getInputStream(sstEntry);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(is, "UTF-8");
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && ("t".equals(parser.getName()) || "w:t".equals(parser.getName()))) {
                        parser.next();
                        if (parser.getText() != null) {
                            sharedStrings.add(parser.getText());
                        }
                    }
                    eventType = parser.next();
                }
                is.close();
            }

            // 2. Parse Sheet 1
            ZipEntry sheetEntry = zipFile.getEntry("xl/worksheets/sheet1.xml");
            if (sheetEntry != null) {
                InputStream is = zipFile.getInputStream(sheetEntry);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(is, "UTF-8");
                int eventType = parser.getEventType();
                
                List<String> currentRow = null;
                String currentVal = null;
                boolean isSharedString = false;

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String name = parser.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        if ("row".equals(name)) {
                            currentRow = new ArrayList<>();
                        } else if ("c".equals(name)) {
                            String type = parser.getAttributeValue(null, "t");
                            isSharedString = "s".equals(type);
                        } else if ("v".equals(name)) {
                            parser.next();
                            currentVal = parser.getText();
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if ("row".equals(name)) {
                            if (currentRow != null && !currentRow.isEmpty()) {
                                rows.add(currentRow);
                            }
                            currentRow = null;
                        } else if ("c".equals(name)) {
                            if (currentRow != null) {
                                String cellText = "";
                                if (currentVal != null) {
                                    if (isSharedString) {
                                        try {
                                            int idx = Integer.parseInt(currentVal);
                                            if (idx >= 0 && idx < sharedStrings.size()) {
                                                cellText = sharedStrings.get(idx);
                                            }
                                        } catch (Exception ignored) {}
                                    } else {
                                        cellText = currentVal;
                                    }
                                }
                                currentRow.add(cellText);
                            }
                            currentVal = null;
                            isSharedString = false;
                        }
                    }
                    eventType = parser.next();
                }
                is.close();
            }
        } finally {
            if (zipFile != null) {
                try { zipFile.close(); } catch (Exception ignored) {}
            }
        }

        if (rows.isEmpty()) {
            throw new Exception("Excel sheet contains no readable row/cell entries.");
        }

        // Determine max columns in Excel sheet
        int maxCols = 0;
        for (List<String> row : rows) {
            maxCols = Math.max(maxCols, row.size());
        }
        maxCols = Math.max(1, maxCols);

        // 3. Render Excel data onto a PDF table layout
        PdfDocument document = new PdfDocument();

        int pageWidth = 792; // Use landscape Letter layout for wider excel column spreads
        int pageHeight = 612;
        int margin = 36;
        int contentWidth = pageWidth - (margin * 2);
        int colWidth = contentWidth / maxCols;

        Paint paint = new Paint();
        paint.setTextSize(9f);
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);

        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        Paint headerBgPaint = new Paint();
        headerBgPaint.setColor(Color.parseColor("#ECEFF1"));
        headerBgPaint.setStyle(Paint.Style.FILL);

        int pageNum = 1;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        float y = margin + 20;

        // Title Header
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(14f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("SwiftPDF Sheet Export: " + xlsxFile.getName(), margin, y, titlePaint);
        y += 24;

        for (int rIdx = 0; rIdx < rows.size(); rIdx++) {
            List<String> row = rows.get(rIdx);
            float rowHeight = 20;

            if (y + rowHeight > pageHeight - margin) {
                // Next page
                document.finishPage(page);
                pageNum++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin + 20;
            }

            // Draw header background for row 0 (column names)
            if (rIdx == 0) {
                canvas.drawRect(margin, y, margin + contentWidth, y + rowHeight, headerBgPaint);
            }

            // Draw cells and column vertical lines
            for (int cIdx = 0; cIdx < maxCols; cIdx++) {
                String val = cIdx < row.size() ? row.get(cIdx) : "";
                float x = margin + (cIdx * colWidth);

                // Draw cell contents (clip to column boundaries)
                canvas.save();
                canvas.clipRect(x + 2, y, x + colWidth - 2, y + rowHeight);
                canvas.drawText(val, x + 4, y + 14, paint);
                canvas.restore();

                // Draw grid lines
                canvas.drawRect(x, y, x + colWidth, y + rowHeight, gridPaint);
            }

            y += rowHeight;
        }

        document.finishPage(page);
        FileOutputStream fos = new FileOutputStream(pdfFile);
        document.writeTo(fos);
        fos.flush();
        fos.close();
        document.close();

    }
}
