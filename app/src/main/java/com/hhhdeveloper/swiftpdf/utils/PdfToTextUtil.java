package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileWriter;

public class PdfToTextUtil {

    public static File convertPdfToText(Context context, File pdfFile, String outputFileName) throws Exception {
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            File txtFile = new File(FileUtil.getOutputDirectory(context, "Converted"), outputFileName);
            FileWriter writer = new FileWriter(txtFile);
            writer.write(text);
            writer.flush();
            writer.close();
            return txtFile;
        } finally {
            if (document != null) {
                try { document.close(); } catch (Exception ignored) {}
            }
        }
    }
}
