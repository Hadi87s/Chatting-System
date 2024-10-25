package org.example.udpnetworkproject;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetAddress;
import java.time.LocalDateTime;

public class Message implements Serializable{
    @Serial
    private static final long serialVersionUID = 1L; // Ensure version compatibility
    private String userStatus = "Active";
    private final String originalMessage;
    public Message(String message){
        this.originalMessage = message;
        this.message = message;
        deletedTime = -1;
    }
    private short type;
    private String message;
    private int messageID;
    private LocalDateTime time;
    private int port;
    private long deletedTime;
    private InetAddress address;
    public InetAddress getAddress() {
        return address;
    }
    public void setAddress(InetAddress address) {
        this.address = address;
    }
    public String getOriginalMessage(){
        return originalMessage;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public int getPort() {
        return port;
    }
    public void setDeletedTime(long deletedTime) {
        this.deletedTime = deletedTime;
    }
    public long getDeletedTime() {
        return deletedTime;
    }
    public void setType(short type) {
        this.type = type;
    }
    public short getType() {
        return type;
    }
    public void setMessage(String message){
        this.message = message;
    }
    public void setMessageID (int messageID){
        this.messageID = messageID;
    }
    public void setTime (LocalDateTime time){
        this.time = time;
    }
    public String getMessage (){
        return message;
    }
    public LocalDateTime getTime(){
        return time;
    }
    public int getMessageID(){
        return messageID;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }
}

