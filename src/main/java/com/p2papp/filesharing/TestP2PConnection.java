package com.p2papp.filesharing;

import com.p2papp.filesharing.network.PeerServer;
import com.p2papp.filesharing.network.PeerClient;

public class TestP2PConnection {
    public static void main(String[] args) throws InterruptedException {
        
        // === MÁY 1: Khởi động Server ===
        System.out.println("=== PEER 1: Starting server ===");
        PeerServer server = new PeerServer(8000);
        server.start();
        
        Thread.sleep(2000); // Đợi server khởi động
        
        // === MÁY 2: Kết nối đến Máy 1 ===
        System.out.println("\n=== PEER 2: Connecting to Peer 1 ===");
        PeerClient client = new PeerClient("localhost", 8000); // Đổi "localhost" thành IP máy 1
        
        if (client.connect()) {
            // Test gửi message
            String response1 = client.sendMessage("PING");
            System.out.println("Response: " + response1);
            
            String response2 = client.sendMessage("HELLO");
            System.out.println("Response: " + response2);
            
            String response3 = client.sendMessage("REQUEST_FILE:test.txt");
            System.out.println("Response: " + response3);
            
            client.disconnect();
        }
        
        // Dừng server
        Thread.sleep(2000);
        server.stopServer();
    }
}