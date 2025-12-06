/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.network;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * PeerDiscoveryClient.java - Discover peers trong LAN bằng UDP broadcast
 */
public class PeerDiscoveryClient {
    
    private static final int DISCOVERY_PORT = 8888;
    private static final int TIMEOUT = 3000; // 3 seconds
    
    /**
     * Discover peers trong LAN
     * @return List IP:Port của các peers tìm được
     */
    public static List<String> discoverPeers() {
        List<String> peers = new ArrayList<>();
        
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(TIMEOUT);
            
            // Gửi broadcast message
            byte[] sendData = "DISCOVER".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, 
                sendData.length, 
                InetAddress.getByName("255.255.255.255"), 
                DISCOVERY_PORT
            );
            
            socket.send(sendPacket);
            System.out.println("📡 Broadcasting discovery message...");
            
            // Nhận responses
            byte[] receiveData = new byte[1024];
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < TIMEOUT) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    
                    if (response.startsWith("PEER:")) {
                        String port = response.substring(5);
                        String peerAddress = receivePacket.getAddress().getHostAddress() + ":" + port;
                        peers.add(peerAddress);
                        
                        System.out.println("✅ Found peer: " + peerAddress);
                    }
                    
                } catch (SocketTimeoutException e) {
                    break; // Timeout - no more responses
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ Discovery error: " + e.getMessage());
        }
        
        return peers;
    }
}
