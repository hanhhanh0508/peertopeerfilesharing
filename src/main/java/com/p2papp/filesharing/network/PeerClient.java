/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.network;

import java.io.*;
import java.net.*;


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
        File folder = new File(downloadFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }
   
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
            out.println(message);
            System.out.println("📤 Sent: " + message);
            
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
        
        if (response.startsWith("FILES:")) {
            String fileList = response.substring(6);
            
            if (fileList.equals("NONE") || fileList.isEmpty()) {
                return new String[0];
            }
            
            return fileList.split(",");
        }
        
        return new String[0];
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
public boolean downloadFile(String fileName, String savePath) {
        if (!isConnected) {
            System.err.println("❌ Not connected!");
            return false;
        }
        
        try {
            System.out.println("\n📥 Downloading: " + fileName);
            
            out.println("DOWNLOAD_REQUEST:" + fileName);
            System.out.println("📤 Sent: DOWNLOAD_REQUEST:" + fileName);
            
            String response = in.readLine();
            System.out.println("📥 Received: " + response);
            
            if (response.startsWith("ERROR:")) {
                System.err.println("❌ Server error: " + response);
                return false;
            }
            
            if (!response.startsWith("FILE_SIZE:")) {
                System.err.println("❌ Invalid response: " + response);
                return false;
            }
            
            long fileSize = Long.parseLong(response.split(":")[1]);
            
            System.out.println("   File: " + fileName);
            System.out.println("   Size: " + formatFileSize(fileSize));
            
            File outputFile = new File(savePath, fileName);
            outputFile.getParentFile().mkdirs();
            
            System.out.println("   Saving to: " + outputFile.getAbsolutePath());
            System.out.println("   Progress: 0%");
            
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                InputStream is = socket.getInputStream();
                
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                int bytesRead;
                int lastProgress = 0;
                
                while (totalRead < fileSize) {
                    bytesRead = is.read(buffer);
                    
                    if (bytesRead == -1) {
                        throw new IOException("Unexpected end of stream");
                    }
                    
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                    
                    int progress = (int) ((totalRead * 100) / fileSize);
                    if (progress != lastProgress && progress % 10 == 0) {
                        System.out.print("\r   Progress: " + progress + "%");
                        lastProgress = progress;
                    }
                }
                
                System.out.print("\r   Progress: 100%\n");
            }
            
            System.out.println("✅ Download completed: " + outputFile.getName());
            System.out.println("   Saved to: " + outputFile.getAbsolutePath() + "\n");
            
            return true;
            
        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid file size format");
            return false;
        } catch (IOException e) {
            System.err.println("❌ Download failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Format file size
     */
   
    
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
    // ✅ STATIC METHOD - Download trực tiếp (không cần connect)
    // ============================================
    
    /**
     * Download file trực tiếp từ peer mà không cần tạo instance
     * 
     * @param host IP của peer
     * @param port Port của peer
     * @param fileName Tên file cần download
     * @param savePath Đường dẫn đầy đủ để lưu file (bao gồm tên file)
     * @return true nếu thành công
     */
    public static boolean downloadFileDirect(String host, int port, String fileName, String savePath) {
        Socket socket = null;
        
        try {
            System.out.println("\n📥 Downloading: " + fileName);
            System.out.println("   From: " + host + ":" + port);
            System.out.println("   Save to: " + savePath);
            
            // 1. Kết nối đến peer
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000); // 5s timeout
            
            System.out.println("   ✅ Connected to peer");
            
            // 2. Gửi request
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("DOWNLOAD_REQUEST:" + fileName);
            System.out.println("   📤 Sent: DOWNLOAD_REQUEST:" + fileName);
            
            // 3. Nhận response
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();
            System.out.println("   📥 Received: " + response);
            
            // 4. Kiểm tra response
            if (response == null || response.startsWith("ERROR:")) {
                System.err.println("   ❌ Server error: " + response);
                return false;
            }
            
            if (!response.startsWith("FILE_SIZE:")) {
                System.err.println("   ❌ Invalid response format");
                return false;
            }
            
            // 5. Parse file size
          long fileSize = Long.parseLong(response.split(":")[1]);
          System.out.println("   File size: " + formatFileSize(fileSize));
    
            
            // 6. Nhận binary data
            File outputFile = new File(savePath);
            outputFile.getParentFile().mkdirs();
            
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 InputStream is = socket.getInputStream()) {
                
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                int bytesRead;
                int lastProgress = 0;
                
                System.out.println("   Progress: 0%");
                
                while (totalRead < fileSize) {
                    bytesRead = is.read(buffer);
                    
                    if (bytesRead == -1) {
                        throw new IOException("Unexpected end of stream at " + totalRead + " bytes");
                    }
                    
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                    
                    // Progress bar
                    int progress = (int) ((totalRead * 100) / fileSize);
                    if (progress != lastProgress && progress % 10 == 0) {
                        System.out.print("\r   Progress: " + progress + "%");
                        lastProgress = progress;
                    }
                }
                
                System.out.print("\r   Progress: 100%\n");
                
                if (totalRead != fileSize) {
                    System.err.println("   ⚠️  Warning: Expected " + fileSize + " bytes, received " + totalRead);
                }
            }
            
            System.out.println("   ✅ Download completed!");
            System.out.println("   Saved to: " + outputFile.getAbsolutePath() + "\n");
            
            return true;
            
        } catch (SocketTimeoutException e) {
            System.err.println("   ❌ Connection timeout");
            return false;
        } catch (ConnectException e) {
            System.err.println("   ❌ Connection refused - peer offline?");
            return false;
        } catch (IOException e) {
            System.err.println("   ❌ Download failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (NumberFormatException e) {
            System.err.println("   ❌ Invalid file size format");
            return false;
        } finally {
            // Đóng socket
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
    }
    // ============================================
    // HELPER METHODS
    // ============================================
    
       private static String formatFileSize(long bytes) {
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
    // TEST
    // ============================================
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