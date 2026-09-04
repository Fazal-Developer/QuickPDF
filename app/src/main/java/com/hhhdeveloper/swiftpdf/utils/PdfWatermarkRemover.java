package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;

import com.tom_roush.pdfbox.contentstream.operator.Operator;
import com.tom_roush.pdfbox.cos.COSArray;
import com.tom_roush.pdfbox.cos.COSString;
import com.tom_roush.pdfbox.pdfparser.PDFStreamParser;
import com.tom_roush.pdfbox.pdfwriter.ContentStreamWriter;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDStream;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfWatermarkRemover {

    public static File removeWatermark(Context context, File pdfFile, String watermarkText, String outputFileName) throws Exception {
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile);

            for (PDPage page : document.getPages()) {
                PDFStreamParser parser = new PDFStreamParser(page);
                parser.parse();
                List<Object> tokens = parser.getTokens();
                List<Object> filteredTokens = new ArrayList<>();

                for (int i = 0; i < tokens.size(); i++) {
                    Object token = tokens.get(i);

                    boolean isWatermark = false;

                    if (token instanceof COSString) {
                        String strVal = ((COSString) token).getString();
                        if (strVal.toLowerCase().contains(watermarkText.toLowerCase())) {
                            isWatermark = true;
                        }
                    } else if (token instanceof COSArray) {
                        // Check nested array structures in text drawings
                        COSArray array = (COSArray) token;
                        for (int j = 0; j < array.size(); j++) {
                            if (array.get(j) instanceof COSString) {
                                String strVal = ((COSString) array.get(j)).getString();
                                if (strVal.toLowerCase().contains(watermarkText.toLowerCase())) {
                                    isWatermark = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (isWatermark) {
                        // Skip the text token and its drawing operator
                        if (i + 1 < tokens.size() && tokens.get(i + 1) instanceof Operator) {
                            Operator op = (Operator) tokens.get(i + 1);
                            String opName = op.getName();
                            if ("Tj".equals(opName) || "TJ".equals(opName) || "Do".equals(opName)) {
                                i++; // Skip operator
                            }
                        }
                        continue;
                    }

                    filteredTokens.add(token);
                }

                // Write tokens back to a new stream
                PDStream updatedStream = new PDStream(document);
                OutputStream os = updatedStream.createOutputStream();
                ContentStreamWriter writer = new ContentStreamWriter(os);
                writer.writeTokens(filteredTokens);
                os.flush();
                os.close();

                page.setContents(updatedStream);
            }

            File outputFile = new File(context.getExternalFilesDir(null), outputFileName);
            document.save(outputFile);
            return outputFile;

        } finally {
            if (document != null) {
                try { document.close(); } catch (Exception ignored) {}
            }
        }
    }
}
