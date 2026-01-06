package com.p2papp.filesharing.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * DatabaseConnection.java
 * 
 * Quản lý kết nối đến MySQL database
 * Sử dụng Singleton pattern (chỉ 1 connection duy nhất)
 * 
 * @author P2P Team
 * @version 1.0
 */
public class DatabaseConnection {
    
    // ============================================
    // CONSTANTS - Thông số kết nối
    // ============================================
    
    /**
     * URL của MySQL database
     * Format: jdbc:mysql://[host]:[port]/[database]
     */
    // Trong DatabaseConnection.java
    /*private static final String URL = 
    "jdbc:mysql://10.141.117.107:3306/p2p_file_sharing" +
    "?useUnicode=true" +
    "&characterEncoding=UTF-8" +
    "&serverTimezone=Asia/Ho_Chi_Minh";
    */
    private static final String URL = 
    "jdbc:mysql://localhost:3306/p2p_file_sharing" +
    "?useUnicode=true" +
    "&characterEncoding=UTF-8" +
    "&serverTimezone=Asia/Ho_Chi_Minh";
    
    /**
     * Username MySQL
     */
    private static final String USER = "root";
    
    /**
     * Password MySQL
     * ⚠️ QUAN TRỌNG: ĐỔI PASSWORD CHO ĐÚNG VỚI MYSQL CỦA EM!
     */
    private static final String PASSWORD = "123456";
    
    // ============================================
    // INSTANCE VARIABLE - Singleton
    // ============================================
    
    /**
     * Connection instance (static = dùng chung)
     */
    private static Connection connection = null;
    
    // ============================================
    // PUBLIC METHODS
    // ============================================
    
    /**
     * Lấy kết nối đến database (Singleton pattern)
     * 
     * Kiểm tra:
     * - Nếu connection == null → tạo mới
     * - Nếu connection đã đóng → tạo mới
     * - Nếu connection còn mở → trả về luôn
     * 
     * @return Connection object hoặc null nếu lỗi
     */
    public static Connection getConnection() {
        try {
            // Kiểm tra connection
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                
                // In thông báo thành công
                System.out.println("✅ Database connected successfully!");
            }
            
        } catch (ClassNotFoundException e) {
            // Lỗi: Không tìm thấy MySQL Driver
            // Nguyên nhân: Thiếu mysql-connector-j trong pom.xml
            System.err.println("❌ MySQL Driver not found!");
            System.err.println("   Check if mysql-connector-j is in pom.xml");
            e.printStackTrace();
            
        } catch (SQLException e) {
            // Lỗi: Không kết nối được database
            // Nguyên nhân có thể:
            // 1. MySQL server chưa chạy
            // 2. Sai username/password
            // 3. Database chưa tồn tại
            // 4. Port 3306 bị chặn
            System.err.println("❌ Database connection failed!");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("   SQL State: " + e.getSQLState());
            System.err.println("   Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }
        
        return connection;
    }
    
    /**
     * Đóng kết nối database
     * 
     * Nên gọi khi:
     * - Ứng dụng kết thúc
     * - Cần giải phóng tài nguyên
     */
    public static void closeConnection() {
        try {
            // Kiểm tra connection tồn tại và đang mở
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔴 Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error closing connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ============================================
    // TEST METHODS
    // ============================================
    
    /**
     * Test 1: Kiểm tra kết nối cơ bản
     */
    private static void testConnection() {
        System.out.println("\n=== TEST 1: Basic Connection ===");
        
        Connection conn = getConnection();
        
        if (conn != null) {
            try {
                // Lấy tên database hiện tại
                String dbName = conn.getCatalog();
                System.out.println("   Database: " + dbName);
                
                // Kiểm tra connection còn valid không (timeout 5s)
                boolean isValid = conn.isValid(5);
                System.out.println("   Valid: " + isValid);
                
                // Kiểm tra read-only
                boolean isReadOnly = conn.isReadOnly();
                System.out.println("   Read-only: " + isReadOnly);
                
                System.out.println("✅ Test 1 PASSED!");
                
            } catch (SQLException e) {
                System.err.println("❌ Test 1 FAILED: " + e.getMessage());
            }
        } else {
            System.err.println("❌ Test 1 FAILED: Connection is null");
        }
    }
    
    /**
     * Test 2: Query database info
     */
    private static void testQuery() {
        System.out.println("\n=== TEST 2: Query Database ===");
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Query 1: Lấy thông tin database
            ResultSet rs = stmt.executeQuery(
                "SELECT DATABASE() as db, VERSION() as ver"
            );
            
            if (rs.next()) {
                System.out.println("   Current DB: " + rs.getString("db"));
                System.out.println("   MySQL Ver: " + rs.getString("ver"));
            }
            
            // Query 2: Kiểm tra các bảng
            System.out.println("\n   Tables:");
            String[] tables = {"users", "peers", "files", "downloads"};
            
            for (String table : tables) {
                ResultSet rsTable = stmt.executeQuery(
                    "SELECT COUNT(*) as cnt FROM " + table
                );
                if (rsTable.next()) {
                    int count = rsTable.getInt("cnt");
                    System.out.println("     ✓ " + table + ": " + count + " rows");
                }
            }
            
            System.out.println("\n✅ Test 2 PASSED!");
            
        } catch (SQLException e) {
            System.err.println("❌ Test 2 FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test 3: Đọc dữ liệu users
     */
    private static void testReadData() {
        System.out.println("\n=== TEST 3: Read Users ===");
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            
            int count = 0;
            while (rs.next()) {
                count++;
                int userId = rs.getInt("user_id");
                String username = rs.getString("username");
                String email = rs.getString("email");
                
                System.out.println("   User " + count + ":");
                System.out.println("     ID: " + userId);
                System.out.println("     Username: " + username);
                System.out.println("     Email: " + email);
            }
            
            System.out.println("\n   Total: " + count + " users");
            System.out.println("✅ Test 3 PASSED!");
            
        } catch (SQLException e) {
            System.err.println("❌ Test 3 FAILED: " + e.getMessage());
        }
    }
    
    // ============================================
    // MAIN - Chạy tất cả tests
    // ============================================
    
    /**
     * Main method - Test suite
     * 
     * Cách chạy:
     * mvn compile exec:java -Dexec.mainClass="com.p2papp.filesharing.database.DatabaseConnection"
     */
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║  DATABASE CONNECTION TEST SUITE      ║");
        System.out.println("║  P2P File Sharing Application        ║");
        System.out.println("╚══════════════════════════════════════╝");
        
        try {
            // Chạy các tests
            testConnection();
            testQuery();
            testReadData();
            
            // Tất cả tests pass
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║       ✅ ALL TESTS PASSED! ✅        ║");
            System.out.println("╚══════════════════════════════════════╝\n");
            
        } catch (Exception e) {
            System.err.println("\n❌ TEST SUITE FAILED!");
            e.printStackTrace();
            
        } finally {
            // Luôn đóng connection
            closeConnection();
        }
    }
}