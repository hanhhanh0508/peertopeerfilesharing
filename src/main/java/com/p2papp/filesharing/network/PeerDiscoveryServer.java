/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.network;

import java.net.*;
import java.io.*;

/**
 * PeerDiscoveryServer.java - UDP Broadcast để discover peers trong LAN
 * 
 * Lắng nghe UDP broadcasts từ các peer khác
 */
public class PeerDiscoveryServer extends Thread {
    
    private static final int DISCOVERY_PORT = 8888;
    private DatagramSocket socket;
    private boolean isRunning = false;
    private int serverPort; // Port của PeerServer
    
    public PeerDiscoveryServer(int serverPort) {
        this.serverPort = serverPort;
    }
    
    @Override
    public void run() {
        try {
            socket = new DatagramSocket(DISCOVERY_PORT);
            isRunning = true;
            
            System.out.println("🔍 Discovery Server listening on port " + DISCOVERY_PORT);
            
            byte[] buffer = new byte[1024];
            
            while (isRunning) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength());
                
                if (message.equals("DISCOVER")) {
                    // Trả lời với thông tin peer của mình
                    String response = "PEER:" + serverPort;
                    byte[] responseData = response.getBytes();
                    
                    DatagramPacket responsePacket = new DatagramPacket(
                        responseData, 
                        responseData.length, 
                        packet.getAddress(), 
                        packet.getPort()
                    );
                    
                    socket.send(responsePacket);
                    
                    System.out.println("📡 Responded to discovery from: " + packet.getAddress());
                }
            }
            
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("❌ Discovery server error: " + e.getMessage());
            }
        }
    }
    
    public void stopServer() {
        isRunning = false;
        if (socket != null) {
            socket.close();
        }
    }
}
