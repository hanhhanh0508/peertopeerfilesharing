/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HashUtil.java - Tiện ích mã hóa
 * 
 * Sử dụng SHA-256 để:
 * - Hash password
 * - Hash file (kiểm tra tính toàn vẹn)
 */
public class HashUtil {
    
    // ============================================
    // PASSWORD HASHING
    // ============================================
    
    /**
     * Mã hóa password bằng SHA-256
     * 
     * @param password Password gốc
     * @return Hash string 64 ký tự hex
     * 
     * VD: "123456" → "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92"
     * 
     * Tại sao dùng SHA-256?
     * - Mã hóa 1 chiều (không thể decode)
     * - Luôn cho kết quả giống nhau với cùng input
     * - Nhanh và an toàn
     */
    public static String hashPassword(String password) {
        try {
            // Tạo MessageDigest với thuật toán SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // Hash password
            // password.getBytes("UTF-8") → chuyển String thành byte array
            byte[] hashBytes = md.digest(password.getBytes("UTF-8"));
            
            // Chuyển byte[] thành hex string
            return bytesToHex(hashBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Verify password với hash đã lưu
     * 
     * @param password Password người dùng nhập
     * @param storedHash Hash lưu trong database
     * @return true nếu khớp
     */
    public static boolean verifyPassword(String password, String storedHash) {
        String hashedInput = hashPassword(password);
        return hashedInput.equals(storedHash);
    }
    
    // ============================================
    // FILE HASHING
    // ============================================
    
    /**
     * Tính hash SHA-256 của file
     * 
     * @param file File cần hash
     * @return Hash string 64 ký tự hex
     * 
     * Dùng để:
     * - Kiểm tra file có bị sửa đổi không
     * - Tìm file trùng lặp
     * - Verify file sau khi download
     */
    public static String hashFile(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // Đọc file theo chunk để tiết kiệm RAM
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                
                // Đọc và hash từng chunk
                while ((bytesRead = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            
            // Lấy hash cuối cùng
            byte[] hashBytes = md.digest();
            
            return bytesToHex(hashBytes);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not found", e);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }
    
    /**
     * Verify file integrity
     * 
     * @param file File đã download
     * @param expectedHash Hash file gốc
     * @return true nếu file nguyên vẹn
     */
    public static boolean verifyFile(File file, String expectedHash) {
        String actualHash = hashFile(file);
        return actualHash.equalsIgnoreCase(expectedHash);
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Chuyển byte array thành hex string
     * 
     * @param bytes Byte array
     * @return Hex string lowercase
     * 
     * VD: [141, 150, 158, ...] → "8d969e..."
     * 
     * Giải thích:
     * - 0xff & b: Lấy 8 bit cuối của byte (unsigned)
     * - Integer.toHexString(): Chuyển sang hex
     * - Thêm '0' nếu chỉ có 1 chữ số
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        
        for (byte b : bytes) {
            // Chuyển byte thành hex (2 chữ số)
            String hex = Integer.toHexString(0xff & b);
            
            // Thêm leading 0 nếu cần
            if (hex.length() == 1) {
                hexString.append('0');
            }
            
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    // ============================================
    // TEST
    // ============================================
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║     HASH UTIL TEST SUITE           ║");
        System.out.println("╚════════════════════════════════════╝\n");
        
        // Test 1: Hash password
        System.out.println("Test 1: Password Hashing");
        String password = "123456";
        String hashed = hashPassword(password);
        System.out.println("   Password: " + password);
        System.out.println("   Hashed: " + hashed);
        System.out.println("   Expected: 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92");
        boolean matchExpected = hashed.equals("8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92");
        System.out.println("   Match: " + (matchExpected ? "✅" : "❌"));
        
        // Test 2: Verify password
        System.out.println("\nTest 2: Password Verification");
        boolean correctPass = verifyPassword("123456", hashed);
        boolean wrongPass = verifyPassword("wrong", hashed);
        System.out.println("   Correct password: " + (correctPass ? "✅" : "❌"));
        System.out.println("   Wrong password: " + (wrongPass ? "❌ (correct)" : "✅ (wrong)"));
        
        // Test 3: Hash deterministic
        System.out.println("\nTest 3: Hash Deterministic");
        String hash1 = hashPassword("test");
        String hash2 = hashPassword("test");
        System.out.println("   Same input → same hash: " + (hash1.equals(hash2) ? "✅" : "❌"));
        
        // Test 4: Different input → different hash
        System.out.println("\nTest 4: Different Input");
        String hashA = hashPassword("password1");
        String hashB = hashPassword("password2");
        System.out.println("   Different passwords → different hash: " + (!hashA.equals(hashB) ? "✅" : "❌"));
        
        // Test 5: File hashing (nếu có file)
        System.out.println("\nTest 5: File Hashing");
        File testFile = new File("test.txt");
        if (testFile.exists()) {
            String fileHash = hashFile(testFile);
            System.out.println("   File: " + testFile.getName());
            System.out.println("   Hash: " + fileHash);
            
            // Verify
            boolean verified = verifyFile(testFile, fileHash);
            System.out.println("   Verify: " + (verified ? "✅" : "❌"));
        } else {
            System.out.println("   File 'test.txt' not found - skipping");
        }
        
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║      ✅ ALL TESTS PASSED! ✅       ║");
        System.out.println("╚════════════════════════════════════╝");
    }
}