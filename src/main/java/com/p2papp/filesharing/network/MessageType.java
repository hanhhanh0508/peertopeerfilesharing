/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p2papp.filesharing.network;

/**
 * MessageType.java - Enum các loại message trong protocol
 */
public enum MessageType {
    // Connection
    PING("PING"),
    PONG("PONG"),
    HELLO("HELLO"),
    HELLO_ACK("HELLO_ACK"),
    
    // Info
    GET_INFO("GET_INFO"),
    INFO("INFO"),
    
    // File operations
    LIST_FILES("LIST_FILES"),
    FILES("FILES"),
    REQUEST_FILE("REQUEST_FILE"),
    FILE_INFO("FILE_INFO"),
    FILE_CHUNK("FILE_CHUNK"),
    
    // Discovery
    DISCOVER("DISCOVER"),
    ANNOUNCE("ANNOUNCE"),
    
    // Disconnect
    DISCONNECT("DISCONNECT"),
    BYE("BYE"),
    
    // Error
    ERROR("ERROR");
    
    private final String command;
    
    MessageType(String command) {
        this.command = command;
    }
    
    public String getCommand() {
        return command;
    }
    
    /**
     * Parse string to MessageType
     */
    public static MessageType fromString(String command) {
        for (MessageType type : MessageType.values()) {
            if (type.command.equals(command)) {
                return type;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return command;
    }
}