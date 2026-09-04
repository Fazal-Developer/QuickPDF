package com.hhhdeveloper.swiftpdf.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recent_files")
public class RecentFile {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String fileName;
    private String filePath;
    private long fileSizeBytes;
    private long dateCreated;
    private String operation; // "MERGE", "SPLIT", "COMPRESS", "IMAGE_TO_PDF"

    public RecentFile() {}

    public RecentFile(String fileName, String filePath, long fileSizeBytes,
                      long dateCreated, String operation) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSizeBytes = fileSizeBytes;
        this.dateCreated = dateCreated;
        this.operation = operation;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public long getDateCreated() { return dateCreated; }
    public void setDateCreated(long dateCreated) { this.dateCreated = dateCreated; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getFormattedSize() {
        if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.0f KB", fileSizeBytes / 1024.0);
        } else {
            return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024));
        }
    }

    public String getOperationEmoji() {
        if (operation == null) return "📄";
        switch (operation) {
            case "MERGE": return "🔗";
            case "SPLIT": return "✂️";
            case "COMPRESS": return "🗜️";
            case "IMAGE_TO_PDF": return "🖼️";
            case "LOCK": return "🔒";
            case "UNLOCK": return "🔓";
            case "WATERMARK": return "📝";
            default: return "📄";
        }
    }

    public int getOperationIconRes() {
        if (operation == null) return com.hhhdeveloper.swiftpdf.R.drawable.ic_file_pdf;
        switch (operation) {
            case "MERGE": return com.hhhdeveloper.swiftpdf.R.drawable.ic_merge;
            case "SPLIT": return com.hhhdeveloper.swiftpdf.R.drawable.ic_split;
            case "COMPRESS": return com.hhhdeveloper.swiftpdf.R.drawable.ic_compress;
            case "IMAGE_TO_PDF": return com.hhhdeveloper.swiftpdf.R.drawable.ic_image_to_pdf;
            case "LOCK":
            case "UNLOCK":
                return com.hhhdeveloper.swiftpdf.R.drawable.ic_lock;
            case "WATERMARK":
                return com.hhhdeveloper.swiftpdf.R.drawable.ic_watermark;
            default: return com.hhhdeveloper.swiftpdf.R.drawable.ic_file_pdf;
        }
    }
}
