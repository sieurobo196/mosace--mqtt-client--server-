/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cinatic.lucy.app.test.tool;

/**
 *
 * @author cu.tran
 */
public class AppConfig {
    
    private String mqttsUrl;
    private String topicCam;
    private String userName;
    private String password;
    private String clientId;
    private String topicApp;
    private int runTime;
    private int restTime;
    private boolean isLog = false;
    private long timeCheckStreaming ;
   
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTopicApp() {
        return topicApp;
    }

    public void setTopicApp(String topicApp) {
        this.topicApp = topicApp;
    }

    public String getMqttsUrl() {
        return mqttsUrl;
    }

    public void setMqttsUrl(String mqttsUrl) {
        this.mqttsUrl = mqttsUrl;
    }

    public String getTopicCam() {
        return topicCam;
    }

    public void setTopicCam(String topicCam) {
        this.topicCam = topicCam;
    }

    public int getRunTime() {
        return runTime;
    }

    public void setRunTime(int runTime) {
        this.runTime = runTime;
    }

    public int getRestTime() {
        return restTime;
    }

    public void setRestTime(int restTime) {
        this.restTime = restTime;
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
