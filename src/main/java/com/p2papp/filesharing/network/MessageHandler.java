/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
*/
package com.p2papp.filesharing.network;

/**
 * MessageHandler.java - Xử lý protocol messages
 * 
 * Định nghĩa các message types và helper methods
 */
public class MessageHandler {
    
    // ============================================
    // MESSAGE TYPES - Protocol commands
    // ============================================
    
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    public static final String HELLO = "HELLO";
    public static final String HELLO_ACK = "HELLO_ACK";
    public static final String GET_INFO = "GET_INFO";
    public static final String INFO = "INFO";
    public static final String LIST_FILES = "LIST_FILES";
    public static final String FILES = "FILES";
    public static final String REQUEST_FILE = "REQUEST_FILE";
    public static final String FILE_INFO = "FILE_INFO";
    public static final String DISCONNECT = "DISCONNECT";
    public static final String BYE = "BYE";
    public static final String ERROR = "ERROR";
    
    // ============================================
    // MESSAGE BUILDERS
    // ============================================
    
    /**
     * Tạo HELLO message
     */
    public static String createHelloMessage(String clientName) {
        return HELLO + ":" + clientName;
    }
    
    /**
     * Tạo REQUEST_FILE message
     */
    public static String createRequestFileMessage(String fileName) {
        return REQUEST_FILE + ":" + fileName;
    }
    
    /**
     * Tạo FILE_INFO message
     */
    public static String createFileInfoMessage(String fileName, long fileSize) {
        return FILE_INFO + ":" + fileName + ":" + fileSize;
    }
    
    /**
     * Tạo ERROR message
     */
    public static String createErrorMessage(String errorMsg) {
        return ERROR + ":" + errorMsg;
    }
    
    /**
     * Tạo INFO message
     */
    public static String createInfoMessage(int port, String status, int fileCount) {
        return INFO + ":Port=" + port + ",Status=" + status + ",Files=" + fileCount;
    }
    
    /**
     * Tạo FILES message
     */
    public static String createFilesMessage(String[] fileNames) {
        if (fileNames == null || fileNames.length == 0) {
            return FILES + ":NONE";
        }
        return FILES + ":" + String.join(",", fileNames);
    }
    
    // ============================================
    // MESSAGE PARSERS
    // ============================================
    
    /**
     * Parse FILE_INFO message
     * Format: FILE_INFO:filename:size
     * 
     * @return Array [fileName, fileSize] hoặc null nếu invalid
     */
    public static String[] parseFileInfo(String message) {
        if (message == null || !message.startsWith(FILE_INFO + ":")) {
            return null;
        }
        
        String[] parts = message.split(":", 3);
        if (parts.length < 3) {
            return null;
        }
        
        return new String[] { parts[1], parts[2] };
    }
    
    /**
     * Parse FILES message
     * Format: FILES:file1,file2,file3
     * 
     * @return Array file names hoặc empty array
     */
    public static String[] parseFilesList(String message) {
        if (message == null || !message.startsWith(FILES + ":")) {
            return new String[0];
        }
        
        String fileList = message.substring(FILES.length() + 1);
        
        if (fileList.equals("NONE") || fileList.isEmpty()) {
            return new String[0];
        }
        
        return fileList.split(",");
    }
    
    /**
     * Kiểm tra message có phải error không
     */
    public static boolean isError(String message) {
        return message != null && message.startsWith(ERROR + ":");
    }
    
    /**
     * Lấy error message
     */
    public static String getErrorMessage(String message) {
        if (!isError(message)) {
            return null;
        }
        return message.substring(ERROR.length() + 1);
    }
    
    /**
     * Validate message format
     */
    public static boolean isValidMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        // Check known commands
        String[] validCommands = {
            PING, PONG, HELLO, HELLO_ACK, GET_INFO, INFO,
            LIST_FILES, FILES, REQUEST_FILE, FILE_INFO,
            DISCONNECT, BYE, ERROR
        };
        
        for (String cmd : validCommands) {
            if (message.equals(cmd) || message.startsWith(cmd + ":")) {
                return true;
            }
        }
        
        return false;
    }
}
