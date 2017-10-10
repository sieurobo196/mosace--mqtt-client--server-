/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cinatic.lucy.cam.test.tool;

/**
 *
 * @author NXCOMM
 */
public class CamConfig {

    private ObjectUrlJson objectUrlJson;
    private String password;
    private String userName;
    private String topicCam;
    private String clientId;

    private String getSessionKey;
    private String random;
    private String key;
    private String keyEncrypt;
    private String mqttsUrl;
    private int timeSleepSendData;
    boolean isLog = false;
    private long timeCheckStreaming;

    public int getTimeSleepSendData() {
        return timeSleepSendData;
    }

    public void setTimeSleepSendData(int timeSleepSendData) {
        this.timeSleepSendData = timeSleepSendData;
    }

    public ObjectUrlJson getObjectUrlJson() {
        return objectUrlJson;
    }

    public void setObjectUrlJson(ObjectUrlJson objectUrlJson) {
        this.objectUrlJson = objectUrlJson;
    }

   
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getTopicCam() {
        return topicCam;
    }

    public void setTopicCam(String topicCam) {
        this.topicCam = topicCam;
    }

    public String getGetSessionKey() {
        return getSessionKey;
    }

    public void setGetSessionKey(String getSessionKey) {
        this.getSessionKey = getSessionKey;
    }

    public String getRandom() {
        return random;
    }

    public void setRandom(String random) {
        this.random = random;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKeyEncrypt() {
        return keyEncrypt;
    }

    public void setKeyEncrypt(String keyEncrypt) {
        this.keyEncrypt = keyEncrypt;
    }

    public String getMqttsUrl() {
        return mqttsUrl;
    }

    public void setMqttsUrl(String mqttsUrl) {
        this.mqttsUrl = mqttsUrl;
    }

    public boolean isIsLog() {
        return isLog;
    }

    public void setIsLog(boolean isLog) {
        this.isLog = isLog;
    }

    public long getTimeCheckStreaming() {
        return timeCheckStreaming;
    }

    public void setTimeCheckStreaming(long timeCheckStreaming) {
        this.timeCheckStreaming = timeCheckStreaming;
    }

}
