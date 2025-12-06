/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.database.dao;

import com.p2papp.filesharing.database.DatabaseConnection;
import com.p2papp.filesharing.model.Download;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DownloadDAO.java - DAO cho bảng downloads
 */
public class DownloadDAO {
    
    /**
     * Thêm download mới
     */
    public boolean addDownload(int fileId, int downloaderId) {
        String sql = "INSERT INTO downloads (file_id, downloader_id, status) VALUES (?, ?, 'in_progress')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, fileId);
            pstmt.setInt(2, downloaderId);
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Add download error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Cập nhật trạng thái download
     */
    public boolean updateDownloadStatus(int downloadId, String status) {
        String sql = "UPDATE downloads SET status = ? WHERE download_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, downloadId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Update download status error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Lấy lịch sử download của user
     */
    public List<Download> getDownloadsByUser(int downloaderId) {
        List<Download> downloads = new ArrayList<>();
        
        String sql = "SELECT d.*, f.file_name, f.file_size, u.username as downloader_name " +
                     "FROM downloads d " +
                     "JOIN files f ON d.file_id = f.file_id " +
                     "JOIN users u ON d.downloader_id = u.user_id " +
                     "WHERE d.downloader_id = ? " +
                     "ORDER BY d.download_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, downloaderId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Download download = new Download();
                download.setDownloadId(rs.getInt("download_id"));
                download.setFileId(rs.getInt("file_id"));
                download.setDownloaderId(rs.getInt("downloader_id"));
                download.setDownloadDate(rs.getTimestamp("download_date"));
                download.setStatus(rs.getString("status"));
                download.setFileName(rs.getString("file_name"));
                download.setFileSize(rs.getLong("file_size"));
                download.setDownloaderName(rs.getString("downloader_name"));
                
                downloads.add(download);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Get downloads error: " + e.getMessage());
        }
        
        return downloads;
    }
}
