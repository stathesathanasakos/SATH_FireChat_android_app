package com.example.p22005unipifirechat.modelclasses;

import java.io.Serializable;

public class Message implements Serializable {

    public String senderId;
    public String receiverId;
    public String messageText;
    public long timestamp;
    public String key;

    //Required default constructor for Firebase
    public Message() {
    }

    public Message(String senderId, String receiverId, String messageText, long timestamp, String key) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessageText(){
        return messageText;
    }
}