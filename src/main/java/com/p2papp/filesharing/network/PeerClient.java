/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.network;

import com.p2papp.filesharing.database.DatabaseConnection;
import com.p2papp.filesharing.database.dao.FileDAO;
import com.p2papp.filesharing.model.FileInfo;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * PeerClient.java - Socket Client kết nối đến peer khác
 * 
 * Chức năng:
 * - Kết nối đến peer khác (biết IP:Port)
 * - Gửi requests (PING, LIST_FILES, REQUEST_FILE)
 * - Nhận responses và file
 * - Download file từ peer
 * 
 * @author P2P Team
 */
public class PeerClient {
    
    // ============================================
    // FIELDS
    // ============================================
    
    /**
     * IP address của peer (VD: "192.168.1.10")
     */
    private String peerIP;
    
    /**
     * Port của peer (VD: 8000)
     */
    private int peerPort;
    
    /**
     * Socket connection
     */
    private Socket socket;
    
    /**
     * Input stream - nhận data từ peer
     */
    private BufferedReader in;
    
    /**
     * Output stream - gửi data đến peer
     */
    private PrintWriter out;
    
    /**
     * Trạng thái kết nối
     */
    private boolean isConnected = false;
    
    /**
     * Thư mục lưu file download
     */
    private String downloadFolder = "downloads";
    
    // ============================================
    // CONSTRUCTOR
    // ============================================
    
    /**
     * Constructor
     * @param peerIP IP của peer cần kết nối
     * @param peerPort Port của peer
     */
    public PeerClient(String peerIP, int peerPort) {
        this.peerIP = peerIP;
        this.peerPort = peerPort;
        
   
    }
    
    // ============================================
    // CONNECTION METHODS
    // ============================================
    
    /**
     * Kết nối đến peer
     * 
     * @return true nếu kết nối thành công
     * 
     * Timeout: 5 giây
     * Nếu sau 5 giây không kết nối được → SocketTimeoutException
     */
    public boolean connect() {
        try {
            System.out.println("🔄 Connecting to peer: " + peerIP + ":" + peerPort);
            
            // Tạo socket với timeout
            socket = new Socket();
            socket.connect(new InetSocketAddress(peerIP, peerPort), 5000); // 5s timeout
            
            // Tạo input/output streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true); // auto-flush
            
            isConnected = true;
            System.out.println("✅ Connected to peer: " + peerIP + ":" + peerPort);
            return true;
            
        } catch (SocketTimeoutException e) {
            System.err.println("❌ Connection timeout: Peer không phản hồi");
            System.err.println("   → Kiểm tra peer có đang chạy không");
            System.err.println("   → Kiểm tra firewall");
            return false;
            
        } catch (ConnectException e) {
            System.err.println("❌ Connection refused: Peer từ chối kết nối");
            System.err.println("   → Peer không online hoặc port sai");
            return false;
            
        } catch (UnknownHostException e) {
            System.err.println("❌ Unknown host: IP address không hợp lệ");
            System.err.println("   → Kiểm tra lại IP: " + peerIP);
            return false;
            
        } catch (IOException e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Ngắt kết nối
     */
    public void disconnect() {
        try {
            // Gửi DISCONNECT trước khi đóng
            if (isConnected && out != null) {
                sendMessage("DISCONNECT");
            }
            
            // Đóng streams và socket
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            isConnected = false;
            System.out.println("🔴 Disconnected from peer");
            
        } catch (IOException e) {
            System.err.println("❌ Disconnect error: " + e.getMessage());
        }
    }
    
    // ============================================
    // COMMUNICATION METHODS
    // ============================================
    
    /**
     * Gửi message đến peer và nhận response
     * 
     * @param message Message cần gửi
     * @return Response từ peer, null nếu lỗi
     */
    public String sendMessage(String message) {
        if (!isConnected) {
            System.err.println("❌ Not connected to peer!");
            return null;
        }
        
        try {
            // Gửi message
            out.println(message);
            System.out.println("📤 Sent: " + message);
            
            // Nhận response
            String response = in.readLine();
            System.out.println("📥 Received: " + response);
            
            return response;
            
        } catch (IOException e) {
            System.err.println("❌ Send/Receive error: " + e.getMessage());
            isConnected = false;
            return null;
        }
    }
    
    // ============================================
    // PROTOCOL COMMANDS
    // ============================================
    
    /**
     * PING - Test connection
     * @return true nếu peer phản hồi PONG
     */
    public boolean ping() {
        String response = sendMessage("PING");
        return "PONG".equals(response);
    }
    
    /**
     * HELLO - Greeting
     * @param clientName Tên client
     * @return Response từ server
     */
    public String sayHello(String clientName) {
        return sendMessage("HELLO:" + clientName);
    }
    
    /**
     * GET_INFO - Lấy thông tin server
     * @return Server info string
     */
    public String getServerInfo() {
        return sendMessage("GET_INFO");
    }
    
    /**
     * LIST_FILES - Lấy danh sách file
     * @return Array tên file, empty array nếu không có
     */
    public String[] getFileList() {
        String response = sendMessage("LIST_FILES");
        
        if (response == null) {
            return new String[0];
        }
        
        // Parse response: "FILES:file1.txt,file2.pdf"
        if (response.startsWith("FILES:")) {
            String fileList = response.substring(6); // Bỏ "FILES:"
            
            if (fileList.equals("NONE") || fileList.isEmpty()) {
                return new String[0];
            }
            
            return fileList.split(",");
        }
        
        return new String[0];
    }
    /**
     * REQUEST_FILE_DOWNLOAD - Gửi request tải file nhưng CHƯA tải ngay
     * Chỉ trả về thông tin file nếu tồn tại
     * @return FileInfo -> name + size, hoặc null nếu lỗi.
     */
    public FileInfo requestFileDownload(String fileName){
        if(!isConnected){
            System.err.println(" Not connected!");
            return null;
        }
        try {
            out.println("REQUEST_FILE:" + fileName);
            System.out.println("Sent: REQUEST_FILE:" + fileName);
            String response = in.readLine();
            System.out.println("Received: " + response);
            if (response.startsWith("ERROR:")) {
                System.err.println(" Peer error: " + response);
                return null;
            }
            //Expected: FILE_INFO:name:size
            if (!response.startsWith("FILE_INFO:")) {
                System.err.println(" Invalid response format");
                return null;
            }
            String[] parts = response.split(":");
            String name = parts[1];
            long size = Long.parseLong(parts[2]);
            return new FileInfo(name,size);
        } catch (Exception e){
            System.err.println("Request failed: "+e.getMessage());
            return null;
        }
    }
    // ============================================
    // FILE DOWNLOAD
    // ============================================
    
    /**
     * Download file từ peer
     * 
     * @param fileName Tên file cần download
     * @return true nếu download thành công
     * 
     * Flow:
     * 1. Gửi REQUEST_FILE:filename
     * 2. Nhận FILE_INFO:name:size
     * 3. Nhận binary data
     * 4. Lưu vào downloads/filename
     */
    public boolean downloadFile(String fileName) {
        return downloadFile(fileName, downloadFolder);
    }
    
  /**
 * Download file từ peer, tương thích với tên file có dấu/không chuẩn
 * @param fileName Tên file cần download (gốc)
 * @param saveDir Thư mục lưu file
 * @return true nếu download thành công
 */
public boolean downloadFile(String fileName, String saveDir) {
    try (Socket socket = new Socket(peerIP, peerPort);
       PrintWriter out = new PrintWriter(
        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
        true
);

         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8))) {

        // Gửi request file
        out.println("REQUEST_FILE:" + fileName);
        System.out.println("Sent REQUEST_FILE:" + fileName);

        String response = in.readLine();
        if (response == null || !response.startsWith("FILE_INFO:")) {
            System.err.println("❌ File not found or invalid response: " + response);
            return false;
        }

        String[] meta = response.split(":");
        long fileSize = Long.parseLong(meta[2]);

        // Tạo thư mục lưu file nếu chưa tồn tại
        File dir = new File(saveDir);
        if (!dir.exists()) dir.mkdirs();

        // Lưu file
        File saveFile = new File(dir, fileName);
        try (InputStream rawIn = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream(saveFile)) {

            byte[] buffer = new byte[4096];
            long received = 0;
            int read;

            while (received < fileSize && (read = rawIn.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                received += read;
            }

            fos.flush();
        }

        System.out.println("📥 Downloaded → " + saveFile.getAbsolutePath());
        return true;

    } catch (Exception e) {
        System.err.println("❌ Download failed for '" + fileName + "'");
        e.printStackTrace();
        return false;
    }
}

    /**
 * Download file trực tiếp bằng host + port mà không cần connect() trước
 */
  /*
    public boolean downloadFile(String host, int port, String fileName, String savePath) {
    try (Socket socket = new Socket(host, port)) {

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Gửi yêu cầu
        out.println("DOWNLOAD_REQUEST:" + fileName);

        // Nhận kích thước file
        String response = in.readLine();
        if (!response.startsWith("FILE_SIZE:")) return false;

        long fileSize = Long.parseLong(response.split(":")[1]);

        // Nhận dữ liệu nhị phân
        InputStream is = socket.getInputStream();
        FileOutputStream fos = new FileOutputStream(savePath);

        byte[] buffer = new byte[4096];
        long total = 0;
        int read;

        while (total < fileSize && (read = is.read(buffer)) > 0) {
            fos.write(buffer, 0, read);
            total += read;
        }

        fos.close();
        System.out.println("📥 Download completed -> " + savePath);
        return true;

    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
    }
*/
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Format file size
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    // ============================================
    // GETTERS & SETTERS
    // ============================================
    
    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected() && !socket.isClosed();
    }
    
    public String getPeerIP() {
        return peerIP;
    }
    
    public int getPeerPort() {
        return peerPort;
    }
    
    public String getDownloadFolder() {
        return downloadFolder;
    }
    
    public void setDownloadFolder(String downloadFolder) {
        this.downloadFolder = downloadFolder;
    }
    
    // ============================================
    // TEST
    // ============================================
    // ============================================
// DOWNLOAD WITH PROGRESS (dùng cho JavaFX ProgressBar)
// ============================================
/**
 * Download file từ peer và lưu record vào bảng downloads
 * @param peerIp IP peer gửi file
 * @param peerPort Port peer gửi file
 * @param fileName Tên file cần download
 * @param saveDir Thư mục lưu file
 * @param downloaderId ID của user đang download
 * @return true nếu download thành công
 */
public boolean downloadFileWithRecord(String peerIp, int peerPort, String fileName, String saveDir, int downloaderId) {
    boolean success = false;
    FileInfo fileInfo = null;

    try (Socket socket = new Socket(peerIp, peerPort);
         PrintWriter out = new PrintWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
         BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

        // Gửi request file
        out.println("REQUEST_FILE:" + fileName);
        System.out.println("Sent REQUEST_FILE: " + fileName);

        String response = in.readLine();
        if (response == null || !response.startsWith("FILE_INFO:")) {
            System.err.println("❌ File not found or invalid response: " + response);
            return false;
        }

        // Parse metadata
        String[] meta = response.split(":");
        long fileSize = Long.parseLong(meta[2]);

        // Nhận dữ liệu vào ByteArrayOutputStream để kiểm tra hash trước
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream rawIn = socket.getInputStream();
        byte[] buffer = new byte[4096];
        long received = 0;
        int read;
        while (received < fileSize && (read = rawIn.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
            received += read;

            // Log tiến trình
            double progress = (double) received / fileSize;
            System.out.printf("\r📥 Downloading... %.2f%%", progress * 100);
        }
        System.out.println();

        byte[] fileData = baos.toByteArray();

        // Lấy thông tin file từ DB để so sánh hash
        FileDAO fileDAO = new FileDAO();
        fileInfo = fileDAO.getFileByName(fileName);
        if (fileInfo == null) {
            System.err.println("⚠️ File not found in DB: " + fileName);
            return false;
        }

        // Tính hash MD5
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hashBytes = digest.digest(fileData);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) sb.append(String.format("%02x", b));
        String downloadedHash = sb.toString();

        // So sánh hash
        if (!downloadedHash.equalsIgnoreCase(fileInfo.getFileHash())) {
            System.err.println("❌ File tải về không khớp với DB: " + fileName);
            success = false;
        } else {
            // Lưu file
            File outFile = new File(saveDir, fileName);
            outFile.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(fileData);
            }
            System.out.println("✅ FILE DOWNLOADED AND VERIFIED → " + outFile.getAbsolutePath());
            success = true;
        }

    } catch (Exception e) {
        System.err.println("❌ Download failed for '" + fileName + "'");
        e.printStackTrace();
        success = false;
    } finally {
        // Cập nhật vào bảng downloads
        if (fileInfo != null) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO downloads(file_id, downloader_id, download_date, status) VALUES (?, ?, ?, ?)")) {

                ps.setInt(1, fileInfo.getFileId());
                ps.setInt(2, downloaderId);
                ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                ps.setString(4, success ? "completed" : "failed");
                ps.executeUpdate();

                System.out.println("✅ Download record saved to DB: " + fileName + " -> " + (success ? "completed" : "failed"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("⚠️ Could not save download record (file not found in DB): " + fileName);
        }
    }

    return success;
}




    /**
     * Main method - Test client
     * 
     * ⚠️ QUAN TRỌNG: Phải có PeerServer đang chạy trước!
     * 
     * Cách test:
     * Terminal 1: mvn exec:java -Dexec.mainClass="...PeerServer"
     * Terminal 2: mvn exec:java -Dexec.mainClass="...PeerClient"
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║       PEER CLIENT TEST                 ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        // ⚠️ ĐỔI IP NÀY KHI TEST 2 MÁY THẬT!
        // Localhost: "localhost" hoặc "127.0.0.1"
        // Máy khác: "192.168.1.10" (IP của máy chạy PeerServer)
        String serverIP = "localhost";
        int serverPort = 8000;
        
        PeerClient client = new PeerClient(serverIP, serverPort);
        
        // Kết nối
        if (client.connect()) {
            
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║   TESTING COMMANDS                     ║");
            System.out.println("╚════════════════════════════════════════╝\n");
            
            // Test 1: PING
            System.out.println("【 Test 1: PING 】");
            boolean pingOK = client.ping();
            System.out.println("Result: " + (pingOK ? "✅ Success" : "❌ Failed"));
            Thread.sleep(1000);
            
            // Test 2: HELLO
            System.out.println("\n【 Test 2: HELLO 】");
            String helloResponse = client.sayHello("TestClient");
            System.out.println("Result: " + (helloResponse != null ? "✅" : "❌"));
            Thread.sleep(1000);
            
            // Test 3: GET_INFO
            System.out.println("\n【 Test 3: GET_INFO 】");
            String info = client.getServerInfo();
            System.out.println("Result: " + (info != null ? "✅" : "❌"));
            Thread.sleep(1000);
            
            // Test 4: LIST_FILES
            System.out.println("\n【 Test 4: LIST_FILES 】");
            String[] files = client.getFileList();
            System.out.println("Files available on peer:");
            if (files.length == 0) {
                System.out.println("  (No files)");
            } else {
                for (String file : files) {
                    System.out.println("  - " + file);
                }
            }
            Thread.sleep(1000);
            
            // Test 5: DOWNLOAD FILE
            System.out.println("\n【 Test 5: DOWNLOAD FILE 】");
            if (files.length > 0) {
                String fileToDownload = files[0];
                System.out.println("Downloading: " + fileToDownload);
                boolean downloadOK = client.downloadFile(fileToDownload);
                System.out.println("Result: " + (downloadOK ? "✅ Success" : "❌ Failed"));
            } else {
                System.out.println("⚠️  No files to download");
                System.out.println("   Create test file:");
                System.out.println("   echo 'Hello' > shared_files/test.txt");
            }
            
            // Disconnect
            Thread.sleep(2000);
            client.disconnect();
            
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║      ✅ ALL TESTS COMPLETED! ✅        ║");
            System.out.println("╚════════════════════════════════════════╝");
            
        } else {
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║    ❌ CANNOT CONNECT TO PEER! ❌       ║");
            System.out.println("╚════════════════════════════════════════╝\n");
            
            System.out.println("📝 Troubleshooting:");
            System.out.println("   1. Make sure PeerServer is running:");
            System.out.println("      mvn exec:java -Dexec.mainClass=\"com.p2papp.filesharing.network.PeerServer\"");
            System.out.println("   2. Check IP address: " + serverIP);
            System.out.println("   3. Check port: " + serverPort);
            System.out.println("   4. Check firewall settings");
        }
    }
}