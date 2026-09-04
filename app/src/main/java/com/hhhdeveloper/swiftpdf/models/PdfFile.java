package com.hhhdeveloper.swiftpdf.models;

public class PdfFile {
    private String name;
    private String path;
    private String uri;
    private long sizeBytes;

    public PdfFile() {}

    public PdfFile(String name, String path, String uri, long sizeBytes) {
        this.name = name;
        this.path = path;
        this.uri = uri;
        this.sizeBytes = sizeBytes;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getFormattedSize() {
        if (sizeBytes < 1024 * 1024) {
            return String.format("%.0f KB", sizeBytes / 1024.0);
        } else {
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
        }
    }
}
