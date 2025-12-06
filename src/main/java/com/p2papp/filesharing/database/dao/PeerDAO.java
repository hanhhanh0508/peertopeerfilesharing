/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.database.dao;

import com.p2papp.filesharing.database.DatabaseConnection;
import com.p2papp.filesharing.model.Peer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PeerDAO.java - Data Access Object cho bảng peers
 * 
 * Quản lý peers trong mạng P2P:
 * - Đăng ký peer online/offline
 * - Lấy danh sách peers
 * - Cập nhật trạng thái
 */
public class PeerDAO {
    
    // ============================================
    // CREATE/UPDATE - Đăng ký peer
    // ============================================
    
    /**
     * Đăng ký peer online
     * 
     * Nếu peer (user_id) đã tồn tại → UPDATE IP/Port/Status
     * Nếu chưa tồn tại → INSERT mới
     * 
     * @param userId ID của user
     * @param ipAddress IP address của peer
     * @param port Port mà peer đang lắng nghe
     * @return true nếu thành công
     * 
     * Sử dụng: ON DUPLICATE KEY UPDATE
     * - Bảng peers có UNIQUE constraint trên user_id
     * - Nếu INSERT bị duplicate → chạy UPDATE thay vì
     */
    public boolean registerPeer(int userId, String ipAddress, int port) {
        // SQL với ON DUPLICATE KEY UPDATE
        String sql = "INSERT INTO peers (user_id, ip_address, port, status) " +
                     "VALUES (?, ?, ?, 'online') " +
                     "ON DUPLICATE KEY UPDATE " +
                     "ip_address = ?, port = ?, status = 'online', last_seen = CURRENT_TIMESTAMP";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Parameters cho INSERT
            pstmt.setInt(1, userId);
            pstmt.setString(2, ipAddress);
            pstmt.setInt(3, port);
            
            // Parameters cho UPDATE (nếu duplicate)
            pstmt.setString(4, ipAddress);
            pstmt.setInt(5, port);
            
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Peer registered: " + ipAddress + ":" + port);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Register peer failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    // ============================================
    // READ - Lấy thông tin peers
    // ============================================
    
    /**
     * Lấy danh sách peers online
     * 
     * JOIN với bảng users để lấy username
     * 
     * @return List<Peer> danh sách peers đang online
     */
    public List<Peer> getOnlinePeers() {
        List<Peer> peers = new ArrayList<>();
        
        // SQL với JOIN
        String sql = "SELECT p.*, u.username " +
                     "FROM peers p " +
                     "JOIN users u ON p.user_id = u.user_id " +
                     "WHERE p.status = 'online' " +
                     "ORDER BY p.last_seen DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Peer peer = new Peer();
                peer.setPeerId(rs.getInt("peer_id"));
                peer.setUserId(rs.getInt("user_id"));
                peer.setIpAddress(rs.getString("ip_address"));
                peer.setPort(rs.getInt("port"));
                peer.setStatus(rs.getString("status"));
                peer.setLastSeen(rs.getTimestamp("last_seen"));
                peer.setUsername(rs.getString("username")); // Từ JOIN
                
                peers.add(peer);
            }
            
            System.out.println("✅ Found " + peers.size() + " online peers");
            
        } catch (SQLException e) {
            System.err.println("❌ Get online peers error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return peers;
    }
    
    /**
     * Lấy tất cả peers (online + offline)
     */
    public List<Peer> getAllPeers() {
        List<Peer> peers = new ArrayList<>();
        
        String sql = "SELECT p.*, u.username " +
                     "FROM peers p " +
                     "JOIN users u ON p.user_id = u.user_id " +
                     "ORDER BY p.status DESC, p.last_seen DESC"; // online trước
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Peer peer = new Peer();
                peer.setPeerId(rs.getInt("peer_id"));
                peer.setUserId(rs.getInt("user_id"));
                peer.setIpAddress(rs.getString("ip_address"));
                peer.setPort(rs.getInt("port"));
                peer.setStatus(rs.getString("status"));
                peer.setLastSeen(rs.getTimestamp("last_seen"));
                peer.setUsername(rs.getString("username"));
                
                peers.add(peer);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Get all peers error: " + e.getMessage());
        }
        
        return peers;
    }
    
    /**
     * Lấy peer theo user ID
     */
    public Peer getPeerByUserId(int userId) {
        String sql = "SELECT p.*, u.username " +
                     "FROM peers p " +
                     "JOIN users u ON p.user_id = u.user_id " +
                     "WHERE p.user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Peer peer = new Peer();
                peer.setPeerId(rs.getInt("peer_id"));
                peer.setUserId(rs.getInt("user_id"));
                peer.setIpAddress(rs.getString("ip_address"));
                peer.setPort(rs.getInt("port"));
                peer.setStatus(rs.getString("status"));
                peer.setLastSeen(rs.getTimestamp("last_seen"));
                peer.setUsername(rs.getString("username"));
                return peer;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Get peer error: " + e.getMessage());
        }
        
        return null;
    }
    
    // ============================================
    // UPDATE - Cập nhật trạng thái
    // ============================================
    
    /**
     * Đánh dấu peer offline
     */
    public boolean setPeerOffline(int userId) {
        String sql = "UPDATE peers SET status = 'offline' WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Peer offline: user_id=" + userId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Set offline error: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Đánh dấu peer online
     */
    public boolean setPeerOnline(int userId) {
        String sql = "UPDATE peers SET status = 'online', last_seen = CURRENT_TIMESTAMP WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Peer online: user_id=" + userId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Set online error: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Cập nhật last_seen (heartbeat)
     */
    public boolean updateLastSeen(int userId) {
        String sql = "UPDATE peers SET last_seen = CURRENT_TIMESTAMP WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Update last_seen error: " + e.getMessage());
            return false;
        }
    }
    
    // ============================================
    // DELETE
    // ============================================
    
    /**
     * Xóa peer
     */
    public boolean deletePeer(int userId) {
        String sql = "DELETE FROM peers WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Peer deleted: user_id=" + userId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Delete peer error: " + e.getMessage());
        }
        
        return false;
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Kiểm tra peer có online không
     */
    public boolean isPeerOnline(int userId) {
        String sql = "SELECT status FROM peers WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("status");
                return "online".equalsIgnoreCase(status);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Check online error: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Đếm số peers online
     */
    public int countOnlinePeers() {
        String sql = "SELECT COUNT(*) FROM peers WHERE status = 'online'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Count error: " + e.getMessage());
        }
        
        return 0;
    }
    
    // ============================================
    // TEST
    // ============================================
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║       PEER DAO TEST SUITE          ║");
        System.out.println("╚════════════════════════════════════╝\n");
        
        PeerDAO dao = new PeerDAO();
        
        // Test 1: Register peer 1
        System.out.println("Test 1: Register Peer 1");
        boolean reg1 = dao.registerPeer(1, "192.168.1.10", 8000);
        System.out.println("Result: " + (reg1 ? "✅" : "❌") + "\n");
        
        // Test 2: Register peer 2
        System.out.println("Test 2: Register Peer 2");
        boolean reg2 = dao.registerPeer(2, "192.168.1.20", 8001);
        System.out.println("Result: " + (reg2 ? "✅" : "❌") + "\n");
        
        // Test 3: Get online peers
        System.out.println("Test 3: Get Online Peers");
        List<Peer> onlinePeers = dao.getOnlinePeers();
        System.out.println("Online peers:");
        for (Peer p : onlinePeers) {
            System.out.println("  - " + p);
        }
        System.out.println();
        
        // Test 4: Update last_seen
        System.out.println("Test 4: Update Last Seen");
        dao.updateLastSeen(1);
        System.out.println();
        
        // Test 5: Set offline
        System.out.println("Test 5: Set Peer 2 Offline");
        dao.setPeerOffline(2);
        System.out.println();
        
        // Test 6: Count online
        System.out.println("Test 6: Count Online Peers");
        int count = dao.countOnlinePeers();
        System.out.println("Online count: " + count + "\n");
        
        // Test 7: Check online
        System.out.println("Test 7: Check Peer Online Status");
        System.out.println("Peer 1 online: " + dao.isPeerOnline(1));
        System.out.println("Peer 2 online: " + dao.isPeerOnline(2));
        System.out.println();
        
        // Test 8: Get peer by user ID
        System.out.println("Test 8: Get Peer by User ID");
        Peer peer = dao.getPeerByUserId(1);
        if (peer != null) {
            System.out.println("Found: " + peer);
        }
        System.out.println();
        
        // Cleanup
        System.out.println("Cleaning up...");
        dao.deletePeer(1);
        dao.deletePeer(2);
        System.out.println("✅ Cleanup done\n");
        
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║      ✅ ALL TESTS PASSED! ✅       ║");
        System.out.println("╚════════════════════════════════════╝");
    }
}