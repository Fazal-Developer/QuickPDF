package com.hhhdeveloper.swiftpdf.utils;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PdfRendererCache {
    private final Map<File, PdfRenderer> cache = new HashMap<>();
    private final Map<File, ParcelFileDescriptor> pfdCache = new HashMap<>();

    public synchronized PdfRenderer getRenderer(File file) throws Exception {
        PdfRenderer renderer = cache.get(file);
        if (renderer == null) {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(pfd);
            cache.put(file, renderer);
            pfdCache.put(file, pfd);
        }
        return renderer;
    }

    public synchronized void closeAll() {
        for (PdfRenderer r : cache.values()) {
            try { r.close(); } catch (Exception ignored) {}
        }
        cache.clear();
        for (ParcelFileDescriptor pfd : pfdCache.values()) {
            try { pfd.close(); } catch (Exception ignored) {}
        }
        pfdCache.clear();
    }
}
