/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.model;

import java.sql.Timestamp;

/**
 * User.java - Model class cho bảng users
 * 
 * Đại diện cho 1 user trong hệ thống
 * Mapping với bảng users trong database
 * 
 */
public class User {
    
    // ============================================
    // FIELDS - Tương ứng với columns trong DB
    // ============================================
    
    /**
     * ID duy nhất của user (PRIMARY KEY)
     */
    private int userId;
    
    /**
     * Tên đăng nhập (UNIQUE)
     */
    private String username;
    
    /**
     * Mật khẩu đã hash SHA-256 (64 ký tự)
     */
    private String passwordHash;
    
    /**
     * Email (UNIQUE)
     */
    private String email;
    
    /**
     * Thời gian tạo tài khoản
     */
    private Timestamp createdAt;
    
    /**
     * Lần đăng nhập cuối cùng
     */
    private Timestamp lastLogin;
    
    // ============================================
    // CONSTRUCTORS
    // ============================================
    
    /**
     * Constructor rỗng (bắt buộc cho JavaBeans)
     */
    public User() {
    }
    
    /**
     * Constructor đầy đủ
     */
    public User(int userId, String username, String passwordHash, String email) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }
    
    /**
     * Constructor cho đăng ký (không có ID)
     */
    public User(String username, String passwordHash, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }
    
    // ============================================
    // GETTERS
    // ============================================
    
    public int getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public String getEmail() {
        return email;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public Timestamp getLastLogin() {
        return lastLogin;
    }
    
    // ============================================
    // SETTERS
    // ============================================
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Override toString() để debug dễ hơn
     */
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", lastLogin=" + lastLogin +
                '}';
    }
    
    /**
     * Override equals() để so sánh 2 user
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return userId == user.userId;
    }
    
    /**
     * Override hashCode() (bắt buộc khi override equals)
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(userId);
    }
    
    // ============================================
    // TEST
    // ============================================
    
    public static void main(String[] args) {
        System.out.println("=== USER MODEL TEST ===\n");
        
        // Tạo user
        User user = new User();
        user.setUserId(1);
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        
        // In ra
        System.out.println(user);
        
        // Test getters
        System.out.println("\nUser ID: " + user.getUserId());
        System.out.println("Username: " + user.getUsername());
        System.out.println("Email: " + user.getEmail());
        
        System.out.println("\n✅ USER MODEL TEST PASSED!");
    }
}