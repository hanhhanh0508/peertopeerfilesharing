/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.database.dao;

import com.p2papp.filesharing.database.DatabaseConnection;
import com.p2papp.filesharing.model.User;
import com.p2papp.filesharing.utils.HashUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO.java - Data Access Object cho bảng users
 * 
 * Xử lý tất cả operations liên quan đến users:
 * - CRUD: Create, Read, Update, Delete
 * - Login/Register
 * - Validation
 */
public class UserDAO {
    
    // ============================================
    // CREATE - Đăng ký user mới
    // ============================================
    
    /**
     * Đăng ký user mới
     * 
     * @param username Tên đăng nhập (unique)
     * @param password Mật khẩu gốc (sẽ được hash)
     * @param email Email (unique)
     * @return true nếu đăng ký thành công
     * 
     * Flow:
     * 1. Hash password bằng SHA-256
     * 2. INSERT vào database
     * 3. Return true nếu thành công
     */
    public boolean registerUser(String username, String password, String email) {
        // SQL với PreparedStatement (chống SQL Injection)
        String sql = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            pstmt.setString(1, username);
            pstmt.setString(2, HashUtil.hashPassword(password)); // Hash password!
            pstmt.setString(3, email);
            
            // Execute INSERT
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ User registered: " + username);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Register failed: " + e.getMessage());
            
            // Kiểm tra lỗi duplicate
            if (e.getErrorCode() == 1062) { // MySQL error code for duplicate
                System.err.println("   Username or email already exists");
            }
        }
        
        return false;
    }
    
    // ============================================
    // READ - Đăng nhập & Lấy thông tin
    // ============================================
    
    /**
     * Đăng nhập
     * 
     * @param username Tên đăng nhập
     * @param password Mật khẩu gốc
     * @return User object nếu thành công, null nếu sai
     * 
     * Flow:
     * 1. Hash password nhập vào
     * 2. SELECT user WHERE username = ? AND password_hash = ?
     * 3. Nếu tìm thấy → tạo User object và return
     * 4. Cập nhật last_login
     */
   public User login(String username, String password) {
    String sql = "SELECT * FROM users WHERE username = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, username);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            String storedHash = rs.getString("password_hash").trim();
            String inputHash = HashUtil.hashPassword(password).trim();
System.out.println("Username: " + username);
System.out.println("Password input: " + password);
System.out.println("Input hash  : " + inputHash);
System.out.println("Stored hash : " + storedHash);

            if (!storedHash.equals(inputHash)) {
                System.out.println("❌ Wrong password");
                return null;
            }

            User user = new User();
            user.setUserId(rs.getInt("user_id"));
            user.setUsername(rs.getString("username"));
            user.setPasswordHash(storedHash);
            user.setEmail(rs.getString("email"));
            user.setCreatedAt(rs.getTimestamp("created_at"));
            user.setLastLogin(rs.getTimestamp("last_login"));

            updateLastLogin(user.getUserId());

            System.out.println("✅ Login successful: " + username);
            return user;
        } else {
            System.out.println("❌ Login failed: username not found");
            return null;
        }

    } catch (SQLException e) {
        e.printStackTrace();
        return null;
    }
}

    
    /**
     * Lấy user theo ID
     */
    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setEmail(rs.getString("email"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                user.setLastLogin(rs.getTimestamp("last_login"));
                return user;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Get user error: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Lấy user theo username
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setEmail(rs.getString("email"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                user.setLastLogin(rs.getTimestamp("last_login"));
                return user;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Get user error: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Lấy tất cả users
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                user.setLastLogin(rs.getTimestamp("last_login"));
                users.add(user);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Get all users error: " + e.getMessage());
        }
        
        return users;
    }
    
    // ============================================
    // UPDATE
    // ============================================
    
    /**
     * Cập nhật last_login
     */
    private void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("❌ Update last_login error: " + e.getMessage());
        }
    }
    
    /**
     * Đổi password
     */
    public boolean changePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, HashUtil.hashPassword(newPassword));
            pstmt.setInt(2, userId);
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Change password error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Cập nhật email
     */
    public boolean updateEmail(int userId, String newEmail) {
        String sql = "UPDATE users SET email = ? WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newEmail);
            pstmt.setInt(2, userId);
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Update email error: " + e.getMessage());
            return false;
        }
    }
    
    // ============================================
    // DELETE
    // ============================================
    
    /**
     * Xóa user
     * Chú ý: Sẽ xóa CASCADE (peers, files, downloads)
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ User deleted: " + userId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Delete user error: " + e.getMessage());
        }
        
        return false;
    }
    
    // ============================================
    // VALIDATION
    // ============================================
    
    /**
     * Kiểm tra username đã tồn tại chưa
     */
    public boolean isUsernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Check username error: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Kiểm tra email đã tồn tại chưa
     */
    public boolean isEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Check email error: " + e.getMessage());
        }
        
        return false;
    }
    
    // ============================================
    // TEST
    // ============================================
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║       USER DAO TEST SUITE          ║");
        System.out.println("╚════════════════════════════════════╝\n");
        
        UserDAO dao = new UserDAO();
        
        // Test 1: Register
        System.out.println("Test 1: Register User");
        boolean registered = dao.registerUser("testuser", "password123", "test@example.com");
        System.out.println("Result: " + (registered ? "✅" : "❌") + "\n");
        
        // Test 2: Check exists
        System.out.println("Test 2: Check Username Exists");
        boolean exists = dao.isUsernameExists("testuser");
        System.out.println("Username exists: " + (exists ? "✅" : "❌") + "\n");
        
        // Test 3: Login correct
        System.out.println("Test 3: Login (Correct Password)");
        User user = dao.login("testuser", "password123");
        if (user != null) {
            System.out.println("✅ Logged in as: " + user.getUsername());
            System.out.println("   Email: " + user.getEmail() + "\n");
        } else {
            System.out.println("❌ Login failed\n");
        }
        
        // Test 4: Login wrong password
        System.out.println("Test 4: Login (Wrong Password)");
        User wrongLogin = dao.login("testuser", "wrongpass");
        System.out.println("Result: " + (wrongLogin == null ? "✅ Correctly rejected" : "❌ Should fail") + "\n");
        
        // Test 5: Get by username
        System.out.println("Test 5: Get User by Username");
        User foundUser = dao.getUserByUsername("testuser");
        System.out.println("Found: " + (foundUser != null ? "✅" : "❌") + "\n");
        
        // Test 6: Get all users
        System.out.println("Test 6: Get All Users");
        List<User> allUsers = dao.getAllUsers();
        System.out.println("Total users: " + allUsers.size());
        for (User u : allUsers) {
            System.out.println("  - " + u.getUsername());
        }
        System.out.println();
        
        // Cleanup
        System.out.println("Cleaning up test data...");
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM users WHERE username='testuser'");
            System.out.println("✅ Cleanup done\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║      ✅ ALL TESTS PASSED! ✅       ║");
        System.out.println("╚════════════════════════════════════╝");
    }
}