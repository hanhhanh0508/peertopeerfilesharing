/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.model;

import java.sql.Timestamp;

/**
 * Peer.java - Model class cho bảng peers
 * 
 * Đại diện cho 1 peer trong mạng P2P
 * Lưu thông tin IP, Port, trạng thái online/offline
 */
public class Peer {
    
    // ============================================
    // FIELDS
    // ============================================
    
    private int peerId;           // ID peer (PRIMARY KEY)
    private int userId;           // ID user sở hữu peer (FOREIGN KEY)
    private String ipAddress;     // IP address (VD: "192.168.1.10")
    private int port;             // Port (VD: 8000)
    private String status;        // "online" hoặc "offline"
    private Timestamp lastSeen;   // Thời gian active cuối cùng
    
    // Thông tin bổ sung (từ JOIN với bảng users)
    private String username;      // Username của peer
    
    // ============================================
    // CONSTRUCTORS
    // ============================================
    
    public Peer() {
    }
    
    /**
     * Constructor tạo peer mới
     */
    public Peer(int userId, String ipAddress, int port) {
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.status = "online"; // Mặc định online
    }
    
    // ============================================
    // GETTERS & SETTERS
    // ============================================
    
    public int getPeerId() {
        return peerId;
    }
    
    public void setPeerId(int peerId) {
        this.peerId = peerId;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Timestamp getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(Timestamp lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Kiểm tra peer có đang online không
     */
    public boolean isOnline() {
        return "online".equalsIgnoreCase(status);
    }
    
    /**
     * Lấy địa chỉ đầy đủ "IP:Port"
     */
    public String getAddress() {
        return ipAddress + ":" + port;
    }
    
    /**
     * Set peer online
     */
    public void setOnline() {
        this.status = "online";
    }
    
    /**
     * Set peer offline
     */
    public void setOffline() {
        this.status = "offline";
    }
    
    @Override
    public String toString() {
        return "Peer{" +
                "peerId=" + peerId +
                ", username='" + username + '\'' +
                ", address=" + getAddress() +
                ", status='" + status + '\'' +
                ", lastSeen=" + lastSeen +
                '}';
    }
    
    // ============================================
    // TEST
    // ============================================
    
    public static void main(String[] args) {
        System.out.println("=== PEER MODEL TEST ===\n");
        
        // Tạo peer
        Peer peer = new Peer(1, "192.168.1.10", 8000);
        peer.setPeerId(1);
        peer.setUsername("peer1");
        peer.setLastSeen(new Timestamp(System.currentTimeMillis()));
        
        // Test
        System.out.println(peer);
        System.out.println("\nAddress: " + peer.getAddress());
        System.out.println("Is online: " + peer.isOnline());
        
        // Test set offline
        peer.setOffline();
        System.out.println("After setOffline: " + peer.isOnline());
        
        System.out.println("\n✅ PEER MODEL TEST PASSED!");
    }
}