package com.p2papp.filesharing.network;

import com.p2papp.filesharing.database.dao.FileDAO;
import com.p2papp.filesharing.model.FileInfo;
import com.p2papp.filesharing.model.User;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * PeerServer.java - Socket Server lắng nghe kết nối từ peer khác
 * 
 * Chức năng:
 * - Lắng nghe trên 1 port (VD: 8000)
 * - Chấp nhận nhiều kết nối đồng thời
 * - Xử lý requests từ peer khác
 * - Gửi/nhận file P2P

 */
public class PeerServer extends Thread {
    
    // ============================================
    // FIELDS
    // ============================================
    
    /**
     * Port lắng nghe (VD: 8000)
     */
    private int port;
    
    /**
     * ServerSocket - socket lắng nghe kết nối
     */
    private ServerSocket serverSocket;
    
    /**
     * Trạng thái server (đang chạy hay không)
     */
    private boolean isRunning = false;
    
    /**
     * Thread pool - xử lý nhiều client đồng thời
     * Tối đa 10 connections cùng lúc
     */
    private ExecutorService threadPool;
    
    /**
     * Thư mục chứa file chia sẻ
     */
    private String sharedFolder = "shared_files";
    
    // ============================================
    // CONSTRUCTOR
    // ============================================
    
    /**
     * Constructor
     * @param port Port để lắng nghe
     */
    public PeerServer(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(10);
        
        // Tạo thư mục shared_files nếu chưa có
        File folder = new File(sharedFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }
    
    // ============================================
    // MAIN THREAD - Accept connections
    // ============================================
    
    /**
     * Thread run method
     * Vòng lặp chấp nhận kết nối từ peer khác
     */
    @Override
    public void run() {
        try {
            // Tạo ServerSocket lắng nghe trên port
            serverSocket = new ServerSocket(port);
            isRunning = true;
            
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║   🟢 PEER SERVER STARTED              ║");
            System.out.println("║   Port: " + port + "                          ║");
            System.out.println("║   Shared folder: " + sharedFolder + "         ║");
            System.out.println("║   Waiting for connections...          ║");
            System.out.println("╚════════════════════════════════════════╝\n");
            
            // Vòng lặp accept connections
            while (isRunning) {
                try {
                    // accept() - blocking call
                    // Chờ đến khi có peer kết nối đến
                    Socket clientSocket = serverSocket.accept();
                    
                    // Lấy thông tin peer
                    String clientIP = clientSocket.getInetAddress().getHostAddress();
                    int clientPort = clientSocket.getPort();
                    
                    System.out.println("📥 New connection from: " + clientIP + ":" + clientPort);
                    
                    // Xử lý client trong thread riêng
                    threadPool.execute(new ClientHandler(clientSocket));
                    
                } catch (SocketException e) {
                    // Server bị stop → SocketException
                    if (!isRunning) break;
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    // ============================================
    // CLIENT HANDLER - Xử lý 1 client
    // ============================================
    
    /**
     * ClientHandler - Xử lý 1 kết nối từ peer
     * Chạy trong thread riêng
     */
    class ClientHandler implements Runnable {
        
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientInfo;
        
        /**
         * Constructor
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }
        
        /**
         * Run method - xử lý client
         */
        @Override
     public void run() {
    try {
        // Tạo input/output streams với UTF-8
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        System.out.println("   ✓ Handler started for: " + clientInfo);

        // Đọc messages từ client
        String message;
        while ((message = in.readLine()) != null) {
            System.out.println("   📩 [" + clientInfo + "] " + message);

            // Xử lý message
            handleMessage(message);

            // Nếu client disconnect
            if (message.equals("DISCONNECT") || message.equals("BYE")) {
                System.out.println("   🔴 Client disconnected: " + clientInfo);
                break;
            }
        }

    } catch (IOException e) {
        System.err.println("   ❌ Handler error [" + clientInfo + "]: " + e.getMessage());
    } finally {
        cleanup();
    }
}

        
        /**
         * Xử lý message theo protocol
         */
        private void handleMessage(String message) {
            try {
                // Validate message
                if (message == null || message.trim().isEmpty()) {
                    sendResponse("ERROR:EMPTY_MESSAGE");
                    return;
                }
                
                // Parse command
                String[] parts = message.split(":", 2);
                String command = parts[0];
                
                // Xử lý từng command
                switch (command) {
                    case "PING":
                        handlePing();
                        break;
                        
                    case "HELLO":
                        handleHello(parts.length > 1 ? parts[1] : "");
                        break;
                        
                    case "GET_INFO":
                        handleGetInfo();
                        break;
                    case "DOWNLOAD_REQUEST":
                    if(parts.length > 1)
                        handleDownloadFile(parts[1]); //parts[1] = fileName
                    else
                        sendResponse("ERROR:MISSING_FILENAME");
                    break;
                    case "LIST_FILES":
                        handleListFiles();
                        break;
                        
                    case "REQUEST_FILE":
                        if (parts.length > 1) {
                            handleRequestFile(parts[1]);
                        } else {
                            sendResponse("ERROR:MISSING_FILENAME");
                        }
                        break;
                        
                    case "DISCONNECT":
                    case "BYE":
                        handleDisconnect();
                        break;
                        
                    default:
                        sendResponse("ERROR:UNKNOWN_COMMAND:" + command);
                }
                
            } catch (Exception e) {
                System.err.println("   ❌ Handle message error: " + e.getMessage());
                sendResponse("ERROR:INTERNAL_ERROR");
            }
        }
        
        // ============================================
        // PROTOCOL HANDLERS
        // ============================================
        
        /**
         * PING - Test connection
         */
        private void handlePing() {
            sendResponse("PONG");
        }
        
        /**
         * HELLO - Greeting
         */
        private void handleHello(String clientName) {
            sendResponse("HELLO_ACK:Welcome " + clientName + "!");
        }
        
        /**
         * GET_INFO - Server info
         */
        private void handleGetInfo() {
            String info = "INFO:Port=" + port + ",Status=Running,Files=" + countFiles();
            sendResponse(info);
        }
     private void handleDownloadFile(String fileName){
    try {
        FileDAO fileDAO = new FileDAO();
        FileInfo fileInfo = fileDAO.getFileByName(fileName);

        if (fileInfo == null) {
            sendResponse("ERROR:FILE_NOT_FOUND");
            return;
        }

        File file = new File(fileInfo.getFilePath());
        if (!file.exists()) {
            sendResponse("ERROR:FILE_NOT_ON_DISK");
            return;
        }

        OutputStream rawOut = socket.getOutputStream();
        PrintWriter textOut = new PrintWriter(rawOut, true);

        // 1️⃣ Gửi metadata trước (TEXT)
        textOut.println("FILE_OK:" + file.length());

        // 2️⃣ Gửi FILE DƯỚI DẠNG BYTES (không PrintWriter nữa)
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int read;

        while ((read = fis.read(buffer)) != -1) {
            rawOut.write(buffer, 0, read);
        }
        rawOut.flush();
        fis.close();

        System.out.println("📤 File sent success → " + fileName);

    } catch (Exception e) {
        sendResponse("ERROR:SEND_FAILED");
        e.printStackTrace();
    }
}

        /**
         * LIST_FILES - Danh sách file có sẵn
         */
        private void handleListFiles() {
            File folder = new File(sharedFolder);
            File[] files = folder.listFiles();
            
            if (files == null || files.length == 0) {
                sendResponse("FILES:NONE");
                return;
            }
            
            // Build file list
            StringBuilder fileList = new StringBuilder("FILES:");
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    fileList.append(files[i].getName());
                    if (i < files.length - 1) {
                        fileList.append(",");
                    }
                }
            }
            
            sendResponse(fileList.toString());
        }
        /**
         * Gửi file binary qua socket (dùng cho DOWNLOAD_REQUEST)
         */
 
        /**
         * REQUEST_FILE - Gửi file cho peer
         */
 // HANDLE REQUEST FROM ANOTHER PEER
private void handleRequestFile(String requestedFileName) {
    System.out.println("DEBUG: requested fileName = '" + requestedFileName + "'");

    try {
        FileDAO fileDAO = new FileDAO();

        // Chuẩn hóa tên file client gửi
        String normalizedName = removeAccent(requestedFileName).toLowerCase().replace(" ", "");

        // Lấy file từ DB dựa trên tên đã chuẩn hóa
        FileInfo fileInfo = fileDAO.getFileByName(normalizedName);

        if (fileInfo == null) {
            sendResponse("ERROR:FILE_NOT_FOUND_IN_DB");
            System.out.println("DEBUG: file not found in DB for '" + requestedFileName + "'");
            return;
        }

        File file = new File(fileInfo.getFilePath());

        if (!file.exists()) {
            sendResponse("ERROR:FILE_NOT_ON_DISK");
            System.out.println("DEBUG: file not found on disk: " + file.getAbsolutePath());
            return;
        }

        sendResponse("FILE_INFO:" + file.getName() + ":" + file.length());
        sendBinaryFile(file);
        System.out.println("📤 SENT FILE → " + file.getAbsolutePath());

    } catch (Exception e) {
        sendResponse("ERROR:SEND_FAILED");
        e.printStackTrace();
    }
}

// Helper: bỏ dấu tiếng Việt
private String removeAccent(String s) {
    if (s == null) return null;
    String temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
    return temp.replaceAll("\\p{M}", "");
}

// Gửi dữ liệu nhị phân
private void sendBinaryFile(File file) throws Exception {
    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
         OutputStream os = socket.getOutputStream()) {

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = bis.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }

        os.flush();
    }
}



        
        /**
         * DISCONNECT - Ngắt kết nối
         */
        private void handleDisconnect() {
            sendResponse("BYE:Connection closed");
        }
        
        // ============================================
        // FILE TRANSFER
        // ============================================
        
        /**
         * Gửi file qua socket
         */
        private void sendFile(File file) throws IOException {
            // Lấy output stream từ socket
            OutputStream os = socket.getOutputStream();
            
            // Đọc file và gửi
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096]; // 4KB buffer
                int bytesRead;
                long totalSent = 0;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;
                    
                    // In progress (mỗi 100KB)
                    if (totalSent % (100 * 1024) == 0) {
                        int progress = (int) ((totalSent * 100) / file.length());
                        System.out.print("\r   Progress: " + progress + "%");
                    }
                }
                
                os.flush();
                System.out.println("\r   Progress: 100%");
            }
        }
        
        // ============================================
        // HELPER METHODS
        // ============================================
        
        /**
         * Gửi text response
         */
        private void sendResponse(String response) {
            out.println(response);
            System.out.println("   📤 [" + clientInfo + "] " + response);
        }
        
        /**
         * Đếm số file trong shared folder
         */
        private int countFiles() {
            File folder = new File(sharedFolder);
            File[] files = folder.listFiles();
            if (files == null) return 0;
            
            int count = 0;
            for (File f : files) {
                if (f.isFile()) count++;
            }
            return count;
        }
        
        /**
         * Cleanup resources
         */
        private void cleanup() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // ============================================
    // SERVER CONTROL
    // ============================================
    
    /**
     * Dừng server
     */
    public void stopServer() {
        System.out.println("\n🔴 Stopping server...");
        isRunning = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            threadPool.shutdown();
            
            // Đợi tối đa 5 giây cho threads kết thúc
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
            
            System.out.println("🔴 Server stopped");
            
        } catch (Exception e) {
            System.err.println("❌ Stop error: " + e.getMessage());
        }
    }
    
    /**
     * Cleanup resources
     */
    private void cleanup() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // ============================================
    // GETTERS
    // ============================================
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getSharedFolder() {
        return sharedFolder;
    }
    
    public void setSharedFolder(String sharedFolder) {
        this.sharedFolder = sharedFolder;
    }
    
    // ============================================
    // TEST - Standalone server
    // ============================================
    
    /**
     * Main method - Test server
     * 
     * Cách chạy:
     * mvn compile exec:java -Dexec.mainClass="com.p2papp.filesharing.network.PeerServer"
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   PEER SERVER TEST                     ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        // Khởi động server
        PeerServer server = new PeerServer(8000);
        server.start();
        
        System.out.println("\n⏳ Server is running...");
        System.out.println("   Press Ctrl+C to stop\n");
        System.out.println("📝 You can test by:");
        System.out.println("   1. telnet localhost 8000");
        System.out.println("   2. Type: PING, HELLO, LIST_FILES");
        System.out.println("   3. Or run PeerClient from another terminal\n");
        
        // Giữ server chạy (trong test thật, dùng vòng lặp vô hạn)
        Thread.sleep(300000); // 5 phút
        
        // Stop server
        server.stopServer();
    }
}