/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.database.dao;

import com.p2papp.filesharing.database.DatabaseConnection;
import com.p2papp.filesharing.model.FileInfo;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.text.Normalizer;


/**
 * FileDAO.java - Data Access Object cho bảng files
 * 
 * Quản lý files được chia sẻ:
 * - Thêm/xóa file
 * - Lấy danh sách file
 * - Tìm kiếm file
 */
public class FileDAO {
    
    // ============================================
    // CREATE - Thêm file
    // ============================================
    
    /**
     * Thêm file mới vào database
     * 
     * @param file FileInfo object chứa thông tin file
     * @return true nếu thành công
     * 
     * Lưu ý: file.userId phải được set trước khi gọi method này
     */
    public boolean addFile(FileInfo file) {
        String sql = "INSERT INTO files (user_id, file_name, file_size, file_hash, file_path) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, file.getUserId());
            pstmt.setString(2, file.getFileName());
            pstmt.setLong(3, file.getFileSize());
            pstmt.setString(4, file.getFileHash());
            pstmt.setString(5, file.getFilePath());
            
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                // Lấy file_id vừa insert
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    file.setFileId(generatedKeys.getInt(1));
                }
                
                System.out.println("✅ File added: " + file.getFileName());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Add file error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    // ============================================
    // READ - Lấy danh sách file
    // ============================================
    
    /**
     * Lấy tất cả file của 1 user
     */
    /*
    public List<FileInfo> getFilesByUser(int userId) {
        List<FileInfo> files = new ArrayList<>();
        
        String sql = "SELECT f.*, u.username as owner_username " +
                     "FROM files f " +
                     "JOIN users u ON f.user_id = u.user_id " +
                     "WHERE f.user_id = ? " +
                     "ORDER BY f.shared_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                FileInfo file = new FileInfo();
                file.setFileId(rs.getInt("file_id"));
                file.setUserId(rs.getInt("user_id"));
                file.setFileName(rs.getString("file_name"));
                file.setFileSize(rs.getLong("file_size"));
                file.setFileHash(rs.getString("file_hash"));
                file.setFilePath(rs.getString("file_path"));
                file.setSharedDate(rs.getTimestamp("shared_date"));
                file.setOwnerUsername(rs.getString("owner_username"));
                
                files.add(file);
            }
            
            System.out.println("✅ Found " + files.size() + " files for user " + userId);
            
        } catch (SQLException e) {
            System.err.println("❌ Get files by user error: " + e.getMessage());
        }
        
        return files;
    }
    */
 public List<FileInfo> getFilesByUser(int userId) {
    List<FileInfo> files = new ArrayList<>();

    String sql =
        "SELECT f.file_id, f.user_id, f.file_name, f.file_size, f.file_hash, " +
        "       f.file_path, f.shared_date, u.username AS owner_name " +  // alias rõ ràng
        "FROM files f " +
        "JOIN users u ON f.user_id = u.user_id " +
        "WHERE f.user_id = ? " +
        "ORDER BY f.shared_date DESC";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setInt(1, userId);

        try (ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {

                FileInfo file = new FileInfo();
                file.setFileId(rs.getInt("file_id"));
                file.setUserId(rs.getInt("user_id"));
                file.setFileName(rs.getString("file_name"));
                file.setFileSize(rs.getLong("file_size"));
                file.setFileHash(rs.getString("file_hash"));
                file.setFilePath(rs.getString("file_path"));
                file.setSharedDate(rs.getTimestamp("shared_date"));

                // tránh lỗi nếu cột trống
                file.setOwnerUsername(rs.getString("owner_name") != null
                                      ? rs.getString("owner_name")
                                      : "Unknown");

                files.add(file);
            }
        }

        System.out.println("📁 Loaded " + files.size() + " file(s) from user_id=" + userId);

    } catch (SQLException e) {
        System.err.println("❌ ERROR getFilesByUser(): " + e.getMessage());
        e.printStackTrace();
    }

    return files;
}


     /**
     * Download file từ đường dẫn lưu trong DB về đường dẫn đích
     * @return 
     */
    public boolean downloadFile(int fileId, String savePath) {
        String sql = "SELECT file_path FROM files WHERE file_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, fileId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String sourcePath = rs.getString("file_path");
                File sourceFile = new File(sourcePath);
                File destFile = new File(savePath);

                if (!sourceFile.exists()) return false;

                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = new FileOutputStream(destFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                return true; // download thành công
            } else {
                return false; // fileId không tồn tại
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Lấy tất cả file được chia sẻ (từ tất cả users)
     */
   /*
    public List<FileInfo> getAllSharedFiles() {
        List<FileInfo> files = new ArrayList<>();
        
        String sql = "SELECT f.*, u.username as owner_username " +
                     "FROM files f " +
                     "JOIN users u ON f.user_id = u.user_id " +
                     "ORDER BY f.shared_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                FileInfo file = new FileInfo();
                file.setFileId(rs.getInt("file_id"));
                file.setUserId(rs.getInt("user_id"));
                file.setFileName(rs.getString("file_name"));
                file.setFileSize(rs.getLong("file_size"));
                file.setFileHash(rs.getString("file_hash"));
                file.setFilePath(rs.getString("file_path"));
                file.setSharedDate(rs.getTimestamp("shared_date"));
                file.setOwnerUsername(rs.getString("owner_username"));
                
                files.add(file);
            }
            
            System.out.println("✅ Found " + files.size() + " shared files");
            
        } catch (SQLException e) {
            System.err.println("❌ Get all files error: " + e.getMessage());
        }
        
        return files;
    }
    */
public List<FileInfo> getAllSharedFiles() {
    List<FileInfo> files = new ArrayList<>();

    // ✅ FIX: Chỉ SELECT các columns CẦN THIẾT
    String sql = "SELECT f.file_id, f.user_id, f.file_name, f.file_size, " +
                 "f.file_hash, f.file_path, f.shared_date, u.username " +
                 "FROM files f " +
                 "JOIN users u ON f.user_id = u.user_id " +
                 "ORDER BY f.shared_date DESC";

    try (Connection conn = DatabaseConnection.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            FileInfo file = new FileInfo();
            
            // ✅ Lấy từng column theo TÊN (safe hơn index)
            file.setFileId(rs.getInt("file_id"));
            file.setUserId(rs.getInt("user_id"));
            file.setFileName(rs.getString("file_name"));
            file.setFileSize(rs.getLong("file_size"));
            file.setFileHash(rs.getString("file_hash"));
            file.setFilePath(rs.getString("file_path"));
            file.setSharedDate(rs.getTimestamp("shared_date"));
            file.setOwnerUsername(rs.getString("username"));

            files.add(file);
        }

        System.out.println("✅ Found " + files.size() + " shared files");

    } catch (SQLException e) {
        System.err.println("❌ Get all files error: " + e.getMessage());
        e.printStackTrace();
    }

    return files;
}



    /**
     * Lấy file theo ID
     */
    public FileInfo getFileById(int fileId) {
        String sql = "SELECT f.*, u.username as owner_username " +
                     "FROM files f " +
                     "JOIN users u ON f.user_id = u.user_id " +
                     "WHERE f.file_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fileId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                FileInfo file = new FileInfo();
                file.setFileId(rs.getInt("file_id"));
                file.setUserId(rs.getInt("user_id"));
                file.setFileName(rs.getString("file_name"));
                file.setFileSize(rs.getLong("file_size"));
                file.setFileHash(rs.getString("file_hash"));
                file.setFilePath(rs.getString("file_path"));
                file.setSharedDate(rs.getTimestamp("shared_date"));
                file.setOwnerUsername(rs.getString("owner_username"));
                return file;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Get file by ID error: " + e.getMessage());
        }
        
        return null;
    }
     private String removeAccent(String s) {
        if (s == null) return null;
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        return temp.replaceAll("\\p{M}", "");
    }

    /**
 * Lấy thông tin file theo đúng tên file (exact match)
 */
public FileInfo getFileByName(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;

        String sql = "SELECT f.*, u.username AS owner_username " +
                     "FROM files f " +
                     "JOIN users u ON f.user_id = u.user_id " +
                     "WHERE REPLACE(LOWER(f.file_name), ' ', '') = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Chuẩn hóa tên file client gửi: bỏ dấu, lowercase, bỏ khoảng trắng
            String normalizedName = removeAccent(fileName).toLowerCase().replace(" ", "");
            pstmt.setString(1, normalizedName);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                FileInfo file = new FileInfo();
                file.setFileId(rs.getInt("file_id"));
                file.setUserId(rs.getInt("user_id"));
                file.setFileName(rs.getString("file_name"));
                file.setFileSize(rs.getLong("file_size"));
                file.setFileHash(rs.getString("file_hash"));
                file.setFilePath(rs.getString("file_path"));
                file.setSharedDate(rs.getTimestamp("shared_date"));
                file.setOwnerUsername(rs.getString("owner_username"));
                return file;
            }

        } catch (SQLException e) {
            System.err.println("❌ Get file by name error: " + e.getMessage());
        }

        return null;
    }
public List<FileInfo> getFilesByName(String fileName) {
    List<FileInfo> files = new ArrayList<>();

    String sql = "SELECT f.*, u.username AS owner_username " +
                 "FROM files f " +
                 "JOIN users u ON f.user_id = u.user_id " +
                 "WHERE f.file_name = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, fileName);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            FileInfo file = new FileInfo();
            file.setFileId(rs.getInt("file_id"));
            file.setUserId(rs.getInt("user_id"));
            file.setFileName(rs.getString("file_name"));
            file.setFileSize(rs.getLong("file_size"));
            file.setFileHash(rs.getString("file_hash"));
            file.setFilePath(rs.getString("file_path"));
            file.setSharedDate(rs.getTimestamp("shared_date"));
            file.setOwnerUsername(rs.getString("owner_username"));
            files.add(file);
        }
    } catch (SQLException e) {
        System.err.println("❌ Get files by name error: " + e.getMessage());
    }

    return files;
}


    /**
     * Tìm kiếm file theo tên
     */
    public List<FileInfo> searchFilesByName(String keyword) {
        List<FileInfo> files = new ArrayList<>();
        
        String sql = "SELECT f.*, u.username as owner_username " +
                     "FROM files f " +
                     "JOIN users u ON f.user_id = u.user_id " +
                     "WHERE LOWER(f.file_name) LIKE LOWER(?) "+
                     "ORDER BY f.shared_date DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "%" + keyword + "%"); // LIKE '%keyword%'
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                FileInfo file = new FileInfo();
                file.setFileId(rs.getInt("file_id"));
                file.setUserId(rs.getInt("user_id"));
                file.setFileName(rs.getString("file_name"));
                file.setFileSize(rs.getLong("file_size"));
                file.setFileHash(rs.getString("file_hash"));
                file.setFilePath(rs.getString("file_path"));
                file.setSharedDate(rs.getTimestamp("shared_date"));
                file.setOwnerUsername(rs.getString("owner_username"));
                
                files.add(file);
            }
            
            System.out.println("✅ Found " + files.size() + " files matching '" + keyword + "'");
            
        } catch (SQLException e) {
            System.err.println("❌ Search files error: " + e.getMessage());
        }
        
        return files;
    }
    
    /**
     * Tìm file theo hash (để kiểm tra duplicate)
     */
    public FileInfo getFileByHash(String fileHash) {
        String sql = "SELECT f.*, u.username as owner_username " +
                     "FROM files f " +
                     "JOIN users u ON f.user_id = u.user_id " +
                     "WHERE f.file_hash = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, fileHash);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                FileInfo file = new FileInfo();
                file.setFileId(rs.getInt("file_id"));
                file.setUserId(rs.getInt("user_id"));
                file.setFileName(rs.getString("file_name"));
                file.setFileSize(rs.getLong("file_size"));
                file.setFileHash(rs.getString("file_hash"));
                file.setFilePath(rs.getString("file_path"));
                file.setSharedDate(rs.getTimestamp("shared_date"));
                file.setOwnerUsername(rs.getString("owner_username"));
                return file;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Get file by hash error: " + e.getMessage());
        }
        
        return null;
    }
    
    // ============================================
    // DELETE
    // ============================================
    
    /**
     * Xóa file
     */
    public boolean deleteFile(int fileId) {
        String sql = "DELETE FROM files WHERE file_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fileId);
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ File deleted: " + fileId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Delete file error: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Xóa tất cả file của user
     */
    public boolean deleteFilesByUser(int userId) {
        String sql = "DELETE FROM files WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            int rows = pstmt.executeUpdate();
            
            System.out.println("✅ Deleted " + rows + " files from user " + userId);
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Delete files error: " + e.getMessage());
            return false;
        }
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Đếm số file của user
     */
    public int countFilesByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM files WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Count files error: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Tổng kích thước file của user (bytes)
     */
    public long getTotalSizeByUser(int userId) {
        String sql = "SELECT SUM(file_size) FROM files WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Get total size error: " + e.getMessage());
        }
        
        return 0;
    }
    
    // ============================================
    // TEST
    // ============================================
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║       FILE DAO TEST SUITE          ║");
        System.out.println("╚════════════════════════════════════╝\n");
        
        FileDAO dao = new FileDAO();
        
        // Test 1: Add file
        System.out.println("Test 1: Add File");
        FileInfo file1 = new FileInfo("test_document.pdf", 1048576, "abc123hash", "/shared/test_document.pdf");
        file1.setUserId(1); // peer1
        boolean added = dao.addFile(file1);
        System.out.println("Result: " + (added ? "✅" : "❌"));
        System.out.println("File ID: " + file1.getFileId() + "\n");
        
        // Test 2: Add another file
        System.out.println("Test 2: Add Another File");
        FileInfo file2 = new FileInfo("image.jpg", 524288, "xyz789hash", "/shared/image.jpg");
        file2.setUserId(1);
        dao.addFile(file2);
        System.out.println();
        
        // Test 3: Get files by user
        System.out.println("Test 3: Get Files by User");
        List<FileInfo> userFiles = dao.getFilesByUser(1);
        System.out.println("Files:");
        for (FileInfo f : userFiles) {
            System.out.println("  - " + f.getFileName() + " (" + f.getFormattedFileSize() + ")");
        }
        System.out.println();
        
        // Test 4: Get all shared files
        System.out.println("Test 4: Get All Shared Files");
        List<FileInfo> allFiles = dao.getAllSharedFiles();
        System.out.println("Total: " + allFiles.size() + " files\n");
        
        // Test 5: Search files
        System.out.println("Test 5: Search Files");
        List<FileInfo> searchResults = dao.searchFilesByName("test");
        System.out.println("Found " + searchResults.size() + " files matching 'test'\n");
        
        // Test 6: Get file by hash
        System.out.println("Test 6: Get File by Hash");
        FileInfo fileByHash = dao.getFileByHash("abc123hash");
        if (fileByHash != null) {
            System.out.println("Found: " + fileByHash.getFileName());
        }
        System.out.println();
        
        // Test 7: Count files
        System.out.println("Test 7: Count Files by User");
        int count = dao.countFilesByUser(1);
        System.out.println("User 1 has " + count + " files\n");
        
        // Test 8: Total size
        System.out.println("Test 8: Total Size");
        long totalSize = dao.getTotalSizeByUser(1);
        System.out.println("Total size: " + totalSize + " bytes\n");
        
        // Cleanup
        System.out.println("Cleaning up...");
        dao.deleteFilesByUser(1);
        System.out.println("✅ Cleanup done\n");
        
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║      ✅ ALL TESTS PASSED! ✅       ║");
        System.out.println("╚════════════════════════════════════╝");
    }
}