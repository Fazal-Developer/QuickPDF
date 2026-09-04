package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import java.io.File;

public class PdfSecurityUtil {

    public interface SecurityCallback {
        void onSuccess(File outputFile);
        void onError(Exception e);
    }

    public static void encrypt(Context context, File srcFile, String password, SecurityCallback callback) {
        try {
            PDDocument document = PDDocument.load(srcFile);
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy(password, password, ap);
            spp.setEncryptionKeyLength(128);
            document.protect(spp);

            File outputDir = FileUtil.getOutputDirectory(context);
            String outName = "locked_" + srcFile.getName();
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

    public static void decrypt(Context context, File srcFile, String password, SecurityCallback callback) {
        try {
            // Load file with password
            PDDocument document = PDDocument.load(srcFile, password);
            if (document.isEncrypted()) {
                document.setAllSecurityToBeRemoved(true);
            }

            File outputDir = FileUtil.getOutputDirectory(context);
            String outName = "unlocked_" + srcFile.getName().replace("locked_", "");
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
