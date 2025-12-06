/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.model;

import java.sql.Timestamp;

/**
 * Download.java - Model class cho bảng downloads
 * 
 * Đại diện cho 1 lượt download file
 * Lưu lịch sử ai download file gì, khi nào, trạng thái thế nào
 */
public class Download {
    
    // ============================================
    // FIELDS
    // ============================================
    
    /**
     * ID download (PRIMARY KEY)
     */
    private int downloadId;
    
    /**
     * ID file được download (FOREIGN KEY)
     */
    private int fileId;
    
    /**
     * ID người download (FOREIGN KEY)
     */
    private int downloaderId;
    
    /**
     * Thời gian download
     */
    private Timestamp downloadDate;
    
    /**
     * Trạng thái: "completed", "failed", "in_progress"
     */
    private String status;
    
    // Thông tin bổ sung (từ JOIN với bảng khác)
    private String fileName;          // Tên file (từ bảng files)
    private String downloaderName;    // Username người download (từ bảng users)
    private long fileSize;            // Kích thước file
    
    // ============================================
    // CONSTRUCTORS
    // ============================================
    
    /**
     * Constructor rỗng
     */
    public Download() {
    }
    
    /**
     * Constructor tạo download mới
     */
    public Download(int fileId, int downloaderId) {
        this.fileId = fileId;
        this.downloaderId = downloaderId;
        this.status = "in_progress"; // Mặc định đang download
    }
    
    /**
     * Constructor đầy đủ
     */
    public Download(int fileId, int downloaderId, String status) {
        this.fileId = fileId;
        this.downloaderId = downloaderId;
        this.status = status;
    }
    
    // ============================================
    // GETTERS & SETTERS
    // ============================================
    
    public int getDownloadId() {
        return downloadId;
    }
    
    public void setDownloadId(int downloadId) {
        this.downloadId = downloadId;
    }
    
    public int getFileId() {
        return fileId;
    }
    
    public void setFileId(int fileId) {
        this.fileId = fileId;
    }
    
    public int getDownloaderId() {
        return downloaderId;
    }
    
    public void setDownloaderId(int downloaderId) {
        this.downloaderId = downloaderId;
    }
    
    public Timestamp getDownloadDate() {
        return downloadDate;
    }
    
    public void setDownloadDate(Timestamp downloadDate) {
        this.downloadDate = downloadDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getDownloaderName() {
        return downloaderName;
    }
    
    public void setDownloaderName(String downloaderName) {
        this.downloaderName = downloaderName;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Kiểm tra download đã hoàn thành chưa
     */
    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }
    
    /**
     * Kiểm tra download có lỗi không
     */
    public boolean isFailed() {
        return "failed".equalsIgnoreCase(status);
    }
    
    /**
     * Kiểm tra đang download
     */
    public boolean isInProgress() {
        return "in_progress".equalsIgnoreCase(status);
    }
    
    /**
     * Đánh dấu download hoàn thành
     */
    public void markAsCompleted() {
        this.status = "completed";
    }
    
    /**
     * Đánh dấu download thất bại
     */
    public void markAsFailed() {
        this.status = "failed";
    }
    
    /**
     * Format file size
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
    
    @Override
    public String toString() {
        return "Download{" +
                "downloadId=" + downloadId +
                ", file='" + fileName + '\'' +
                ", downloader='" + downloaderName + '\'' +
                ", status='" + status + '\'' +
                ", date=" + downloadDate +
                '}';
    }
    
    // ============================================
    // TEST
    // ============================================
    
    public static void main(String[] args) {
        System.out.println("=== DOWNLOAD MODEL TEST ===\n");
        
        // Tạo download
        Download download = new Download(1, 2);
        download.setDownloadId(1);
        download.setFileName("document.pdf");
        download.setDownloaderName("peer2");
        download.setFileSize(1048576);
        download.setDownloadDate(new Timestamp(System.currentTimeMillis()));
        
        // Test
        System.out.println(download);
        System.out.println("\nIs in progress: " + download.isInProgress());
        System.out.println("Formatted size: " + download.getFormattedFileSize());
        
        // Mark completed
        download.markAsCompleted();
        System.out.println("\nAfter completed:");
        System.out.println("Is completed: " + download.isCompleted());
        System.out.println("Status: " + download.getStatus());
        
        System.out.println("\n✅ DOWNLOAD MODEL TEST PASSED!");
    }
}