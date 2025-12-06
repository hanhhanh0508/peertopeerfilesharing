/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.model;

import java.sql.Timestamp;

/**
 * FileInfo.java - Model class cho bảng files
 * 
 * Đại diện cho 1 file được chia sẻ trong hệ thống
 */
public class FileInfo {
    
    // ============================================
    // FIELDS
    // ============================================
    
    private int fileId;           // ID file (PRIMARY KEY)
    private int userId;           // ID user chia sẻ file
    private String fileName;      // Tên file (VD: "document.pdf")
    private long fileSize;        // Kích thước file (bytes)
    private String fileHash;      // Hash SHA-256 của file
    private String filePath;      // Đường dẫn file trên máy
    private Timestamp sharedDate; // Ngày chia sẻ
    
    // Thông tin bổ sung
    private String ownerUsername; // Username của người chia sẻ
    
    // ============================================
    // CONSTRUCTORS
    // ============================================
    
    public FileInfo() {
    }
      // --- Thêm constructor này ---
    public FileInfo(String fileName, long fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
    /**
     * Constructor tạo file mới
     */
    public FileInfo(String fileName, long fileSize, String fileHash, String filePath) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileHash = fileHash;
        this.filePath = filePath;
    }
    
    // ============================================
    // GETTERS & SETTERS
    // ============================================
    
    public int getFileId() {
        return fileId;
    }
    
    public void setFileId(int fileId) {
        this.fileId = fileId;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFileHash() {
        return fileHash;
    }
    
    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Timestamp getSharedDate() {
        return sharedDate;
    }
    
    public void setSharedDate(Timestamp sharedDate) {
        this.sharedDate = sharedDate;
    }
    
    public String getOwnerUsername() {
        return ownerUsername;
    }
    
    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Format file size thành dạng dễ đọc
     * VD: 1024 → "1.00 KB"
     *     1048576 → "1.00 MB"
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * Lấy 16 ký tự đầu của hash (để hiển thị)
     */
    public String getShortHash() {
        if (fileHash != null && fileHash.length() > 16) {
            return fileHash.substring(0, 16) + "...";
        }
        return fileHash;
    }
    
    /**
     * Lấy extension của file
     * VD: "document.pdf" → "pdf"
     */
    public String getExtension() {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }
    
    @Override
    public String toString() {
        return "FileInfo{" +
                "fileId=" + fileId +
                ", fileName='" + fileName + '\'' +
                ", size=" + getFormattedFileSize() +
                ", owner='" + ownerUsername + '\'' +
                ", sharedDate=" + sharedDate +
                '}';
    }
    
    // ============================================
    // TEST
    // ============================================
    
    public static void main(String[] args) {
        System.out.println("=== FILEINFO MODEL TEST ===\n");
        
        // Tạo file
        FileInfo file = new FileInfo("document.pdf", 1048576, "abc123def456", "/shared/document.pdf");
        file.setFileId(1);
        file.setUserId(1);
        file.setOwnerUsername("peer1");
        file.setSharedDate(new Timestamp(System.currentTimeMillis()));
        
        // Test
        System.out.println(file);
        System.out.println("\nFormatted size: " + file.getFormattedFileSize());
        System.out.println("Short hash: " + file.getShortHash());
        System.out.println("Extension: " + file.getExtension());
        
        // Test với file lớn
        FileInfo bigFile = new FileInfo("movie.mp4", 5368709120L, "xyz", "/path");
        System.out.println("\nBig file size: " + bigFile.getFormattedFileSize());
        
        System.out.println("\n✅ FILEINFO MODEL TEST PASSED!");
    }
}
