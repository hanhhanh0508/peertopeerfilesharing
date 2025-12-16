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
        String sql = "INSERT INTO downloads (file_id, downloader_id, status) " +
                     "VALUES (?, ?, 'in_progress')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, fileId);
            pstmt.setInt(2, downloaderId);
            
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int downloadId = rs.getInt(1);
                    System.out.println("✅ Download logged: ID=" + downloadId);
                }
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Add download error: " + e.getMessage());
        }
        
        return false;
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
            
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Download status updated: ID=" + downloadId + ", status=" + status);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Update download status error: " + e.getMessage());
        }
        
        return false;
    }
     public boolean updateDownloadStatusByFileAndUser(int fileId, int downloaderId, String status) {
        String sql = "UPDATE downloads " +
                     "SET status = ? " +
                     "WHERE file_id = ? AND downloader_id = ? " +
                     "ORDER BY download_date DESC " +
                     "LIMIT 1";  // Update download gần nhất
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, fileId);
            pstmt.setInt(3, downloaderId);
            
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Download status updated: fileId=" + fileId + 
                                 ", downloaderId=" + downloaderId + 
                                 ", status=" + status);
                return true;
            } else {
                System.err.println("⚠️  No download found for fileId=" + fileId + 
                                 ", downloaderId=" + downloaderId);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Update download status error: " + e.getMessage());
        }
        
        return false;
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
            
            System.out.println("✅ Found " + downloads.size() + " downloads for user " + downloaderId);
            
        } catch (SQLException e) {
            System.err.println("❌ Get downloads error: " + e.getMessage());
        }
        
        return downloads;
    }
    public Download getLatestDownload(int fileId, int downloaderId) {
        String sql = "SELECT d.*, f.file_name, f.file_size, u.username as downloader_name " +
                     "FROM downloads d " +
                     "JOIN files f ON d.file_id = f.file_id " +
                     "JOIN users u ON d.downloader_id = u.user_id " +
                     "WHERE d.file_id = ? AND d.downloader_id = ? " +
                     "ORDER BY d.download_date DESC " +
                     "LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fileId);
            pstmt.setInt(2, downloaderId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Download download = new Download();
                download.setDownloadId(rs.getInt("download_id"));
                download.setFileId(rs.getInt("file_id"));
                download.setDownloaderId(rs.getInt("downloader_id"));
                download.setDownloadDate(rs.getTimestamp("download_date"));
                download.setStatus(rs.getString("status"));
                download.setFileName(rs.getString("file_name"));
                download.setFileSize(rs.getLong("file_size"));
                download.setDownloaderName(rs.getString("downloader_name"));
                
                return download;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Get latest download error: " + e.getMessage());
        }
        
        return null;
    }
       public boolean hasDownloaded(int fileId, int downloaderId) {
        String sql = "SELECT COUNT(*) FROM downloads " +
                     "WHERE file_id = ? AND downloader_id = ? AND status = 'completed'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fileId);
            pstmt.setInt(2, downloaderId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Check download error: " + e.getMessage());
        }
        
        return false;
    }
     /**
     * ✅ NEW: Đếm số lượt download của 1 file
     */
    public int countDownloadsByFile(int fileId) {
        String sql = "SELECT COUNT(*) FROM downloads " +
                     "WHERE file_id = ? AND status = 'completed'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fileId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Count downloads error: " + e.getMessage());
        }
        
        return 0;
    }
      // ============================================
    // DELETE
    // ============================================
    
    /**
     * Xóa tất cả download history của user
     */
    public boolean deleteDownloadsByUser(int downloaderId) {
        String sql = "DELETE FROM downloads WHERE downloader_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, downloaderId);
            int rows = pstmt.executeUpdate();
            
            System.out.println("✅ Deleted " + rows + " downloads for user " + downloaderId);
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Delete downloads error: " + e.getMessage());
            return false;
        }
    }
     /**
     * Xóa tất cả download history của file
     */
    public boolean deleteDownloadsByFile(int fileId) {
        String sql = "DELETE FROM downloads WHERE file_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fileId);
            int rows = pstmt.executeUpdate();
            
            System.out.println("✅ Deleted " + rows + " downloads for file " + fileId);
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Delete downloads error: " + e.getMessage());
            return false;
        }
    }
}
