package com.hhhdeveloper.swiftpdf.models;

import java.io.File;

public class PageItem {
    public File sourceFile;
    public int originalPageIndex;

    public PageItem(File sourceFile, int originalPageIndex) {
        this.sourceFile = sourceFile;
        this.originalPageIndex = originalPageIndex;
    }
}
