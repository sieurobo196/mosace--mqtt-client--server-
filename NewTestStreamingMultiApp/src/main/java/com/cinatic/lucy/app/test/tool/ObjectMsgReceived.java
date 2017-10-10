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
class ObjectMsgReceived {

        String macAddress;
        int error;
        int portPID;
        String ipPID;
        int portStreaming;
        String ipStreaming;
        String key;
        String random;

        public ObjectMsgReceived() {
        }

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }

        public int getError() {
            return error;
        }

        public void setError(int error) {
            this.error = error;
        }

        public int getPortPID() {
            return portPID;
        }

        public void setPortPID(int portPID) {
            this.portPID = portPID;
        }

        public String getIpPID() {
            return ipPID;
        }

        public void setIpPID(String ipPID) {
            this.ipPID = ipPID;
        }

        public int getPortStreaming() {
            return portStreaming;
        }

        public void setPortStreaming(int portStreaming) {
            this.portStreaming = portStreaming;
        }

        public String getIpStreaming() {
            return ipStreaming;
        }

        public void setIpStreaming(String ipStreaming) {
            this.ipStreaming = ipStreaming;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getRandom() {
            return random;
        }

        public void setRandom(String random) {
            this.random = random;
        }

    }
