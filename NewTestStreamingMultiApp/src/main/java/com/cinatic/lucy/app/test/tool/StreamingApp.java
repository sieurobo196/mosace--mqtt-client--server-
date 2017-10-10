/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cinatic.lucy.app.test.tool;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Logger;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

/**
 *
 * @author cu.tran
 */
public class StreamingApp extends Thread {

    private static final Logger logger = Logger.getLogger(StreamingApp.class);
    private AppConfig appConfig;
    BlockingConnection subscribeConnection = null;
    MQTT mqtt;
    boolean reconnectMqtts = false;
    private final String AUTH = "VLVL";
    boolean isReceived = false;
    boolean isSetMode = false;
    Date startMsg = null;

    public StreamingApp(AppConfig config) {
        appConfig = config;
    }

    @Override
    public void run() {
        int initCount = 0;
        while (true) {
            getConnectMqtts();
            if (initCount == 0 || reconnectMqtts) {
                logger.info(" START StreamingApp------------" + appConfig.getClientId());
                pubMsgToCam();
                String messageReceived = subTopicAndReceiveMsgFromCam();
                ObjectMsgReceived msgReceived = getDataMsgReceived(messageReceived);
                if (msgReceived.getIpStreaming() != null && msgReceived.getIpPID() != null) {
                    sendAccessStreaming(msgReceived);
                    initCount++;
                }
            }
        }
    }

    public void getConnectMqtts() {

        try {
            mqtt = new MQTT();
            mqtt.setHost(appConfig.getMqttsUrl());
            mqtt.setCleanSession(true);
            mqtt.setUserName(appConfig.getUserName());
            mqtt.setPassword(appConfig.getPassword());
            mqtt.setClientId(appConfig.getClientId());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
            mqtt.setSslContext(sslContext);
            if (subscribeConnection == null) {
                subscribeConnection = mqtt.blockingConnection();
                try {
                    subscribeConnection.connect();
                    if (!subscribeConnection.isConnected()) {
                        logger.info(appConfig.getClientId() + "connect fail");
                    } else {
                        logger.info(appConfig.getClientId() + " connect success");
                    }
                } catch (Exception exception) {
                    logger.error("error connect mqtts" + exception);
                }
            }

        } catch (Exception e) {
            logger.error(appConfig.getClientId() + " Error Init Mqtt Connection " + e);

        }
        reconnectMqtts = true;
    }

    public int pubMsgToCam() {
        int countMsg = 0;
        try {
            String msg = "2app_topic_sub=" + appConfig.getTopicApp() + "&time=1496120442102&req=get_session_key&mode=remote&port1=50915&ip=115.78.13.114&streamname=CC3A61D7DEC6_50915";
            subscribeConnection.publish(appConfig.getTopicCam(), msg.getBytes(), QoS.AT_MOST_ONCE, false);
            startMsg = new Date();
//            logger.info(appConfig.getAppId() + " public message :" + msg);
            countMsg = 1;
        } catch (Exception ex) {
            reconnectMqtts = true;
            logger.error(appConfig.getClientId() + " error publish messsage " + ex);
        }
        return countMsg;
    }

    public String subTopicAndReceiveMsgFromCam() {
        String responseMsg = "";
        String subTopicCam = appConfig.getTopicApp();
        Topic topic = new Topic(subTopicCam, QoS.AT_MOST_ONCE);
        Topic[] topics = {topic};
        try {
            subscribeConnection.subscribe(topics);
            Message message = subscribeConnection.receive(30, TimeUnit.SECONDS);
            responseMsg = new String(message.getPayload());
            Date receivedDate = new Date();
            reconnectMqtts = false;
            if (appConfig.isIsLog()) {
                logger.info(appConfig.getClientId() + " received :" + message.getTopic());
            }
            logger.info(appConfig.getClientId() + " received: " + responseMsg);

        } catch (Exception ex) {
            reconnectMqtts = true;
            logger.error(appConfig.getClientId() + " error Timeout received msg " + ex);
        }
        return responseMsg;
    }

    public ObjectMsgReceived getDataMsgReceived(String msgReceived) {
        ObjectMsgReceived objectMsgReceived = new ObjectMsgReceived();
        if (msgReceived != null) {
            String[] msgArray = msgReceived.split("&");
            for (String msgString : msgArray) {
                if (msgString.contains("3id")) {
                    String macAddress = msgString.split(":")[1];
                    objectMsgReceived.setMacAddress(macAddress.trim());
                } else if (msgString.contains("get_session_key")) {
                    String error = msgString.split(":")[1].split(",")[0].split("=")[1];
                    String portPID = msgString.split(":")[1].split(",")[1].split("=")[1];
                    objectMsgReceived.setError(Integer.parseInt(error));
                    objectMsgReceived.setPortPID(Integer.parseInt(portPID));
                } else if (msgString.split("=")[0].equals("ip")) {
                    String ipPID = msgString.split("=")[1];
                    objectMsgReceived.setIpPID(ipPID);
                } else if (msgString.contains("key=")) {
                    String key = msgString.split("=")[1];
                    objectMsgReceived.setKey(key);
                } else if (msgString.split("=")[0].equals("sip")) {
                    String ipStreaming = msgString.split("=")[1];
                    objectMsgReceived.setIpStreaming(ipStreaming);
                } else if (msgString.contains("sp=")) {
                    String portStreaming = msgString.split("=")[1];
                    objectMsgReceived.setPortStreaming(Integer.parseInt(portStreaming));
                } else if (msgString.contains("rn=")) {
                    String random = msgString.split("=")[1];
                    objectMsgReceived.setRandom(random);
                }
            }
        }
        return objectMsgReceived;
    }

    static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    public boolean sendAccessStreaming(ObjectMsgReceived message) {
        boolean isReceived = false;
        String sipStreaming = message.getIpStreaming();
        int portStreaming = message.getPortStreaming();
        String key = message.getKey();
        String rn = message.getRandom();
        String macAddress = message.getMacAddress();
        String ipPID = message.getIpPID();
        int portPID = message.getPortPID();
        if (portPID < 1) {
            ipPID = message.getIpStreaming();
            portPID = message.getPortStreaming();
        }
        if (!sipStreaming.isEmpty() && portStreaming != 0) {

            byte[] outDataAck = createdOutData(50, macAddress, rn, key, 0, "", 0, 0);
            byte[] requestString = createdOutData(30, macAddress, rn, key, 0, ipPID, portPID, 3);
            try {

                while (true) {
                    DatagramSocket datagramSocket = new DatagramSocket();
                    ReceivedStreaming receivedStreaming = new ReceivedStreaming(sipStreaming, portStreaming, datagramSocket);
                    receivedStreaming.setDaemon(true);
                    receivedStreaming.start();
                    byte[] outDataAccess = createdOutData(34, macAddress, rn, key, 0, "", 0, 0);
                    Thread.sleep(5000);
                    sendFrameMsgStreaming(datagramSocket, outDataAccess, sipStreaming, portStreaming, 34);
                    if (reconnectMqtts) {
                        logger.info(appConfig.getClientId() + "----Stop streaming");
                        break;
                    }
                    sendFrameMsgStreaming(datagramSocket, requestString, sipStreaming, portStreaming, 30);
                    boolean isStop = sendFrameMsgStreaming(datagramSocket, outDataAck, sipStreaming, portStreaming, 50);
                    if (isStop) {
                        receivedStreaming.stop();
                        receivedStreaming.stopUDP();
                        logger.info(appConfig.getClientId() + " is sleeping " + appConfig.getRestTime() / 1000);
                        datagramSocket.close();
                    }
                    Thread.sleep(appConfig.getRestTime());

                }
            } catch (SocketException ex) {
                logger.info(appConfig.getClientId() + "Error created datasocket" + ex);
            } catch (Exception e) {
                logger.info(appConfig.getClientId() + "Error thread " + e);
            }

        } else {

            try {
                Thread.sleep(10000);
                reconnectMqtts = true;
            } catch (InterruptedException ex) {
                logger.info(appConfig.getClientId() + "error during sleep " + ex.getMessage());
            }

        }

        return isReceived;

    }

    public byte[] createdOutData(int mediaType, String macAddress, String random, String key,
            int requestCount, String ipPID, int portPID, int valueMode) {
        byte[] authenByte;
        byte[] reserveByte = new byte[1];
        byte[] mediaTypeByte = new byte[1];
        authenByte = AUTH.getBytes();
        String keyEncrypt = "";
        try {
            keyEncrypt = new String(getStringfromHex(key));
        } catch (Exception e) {
            logger.error(appConfig.getClientId() + " error getString to Hex " + e);
            reconnectMqtts = true;
        }
        switch (mediaType) {
            case 34: {
                try {
                    TimeZone timeZone = TimeZone.getTimeZone("UTC");
                    Calendar calendar = Calendar.getInstance(timeZone);
                    SimpleDateFormat simpleDateFormat
                            = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
                    simpleDateFormat.setTimeZone(timeZone);
                    String dateString = simpleDateFormat.format(calendar.getTime());
                    Date date = simpleDateFormat.parse(dateString);
                    int timestamp = (int) (date.getTime() / 1000);
                    byte[] resEncryptByte = new byte[16];
                    if (mediaType == 32) {
                        mediaTypeByte[0] = 32;
                    } else {
                        mediaTypeByte[0] = 34;
                    }
                    byte[] macAddressByte = macAddress.getBytes();

                    try {
                        byte[] randomBumberByte = getStringfromHex(random);
                        byte[] timestampByte = getStringfromHex(Integer.toHexString(timestamp));
                        byte[] msgEncryptByte = getCombinByte(randomBumberByte, timestampByte);
                        String msgEncrypted = new String(msgEncryptByte);
                        try {
                            resEncryptByte = encrypt(msgEncryptByte, keyEncrypt, "AES/CBC/noPadding");
                        } catch (Exception ex) {
                            logger.error(appConfig.getClientId() + " error encrypt exception");
                            reconnectMqtts = true;
                        }
                    } catch (UnsupportedEncodingException ex) {
                        logger.error(appConfig.getClientId() + " error exception getStringfromHex");
                        reconnectMqtts = true;
                    }

                    byte[] totalByte = getCombinByte(authenByte, reserveByte);
                    totalByte = getCombinByte(totalByte, mediaTypeByte);
                    totalByte = getCombinByte(totalByte, macAddressByte);
                    totalByte = getCombinByte(totalByte, resEncryptByte);
                    return totalByte;
                } catch (ParseException ex) {
                    logger.error(appConfig.getClientId() + "ParseException " + ex);
                    reconnectMqtts = true;
                }
            }
            case 50: {
                mediaTypeByte[0] = 50;
                byte[] pidByte = new byte[4];
                byte[] ackByte = new byte[336];
                byte[] ackDataByte = getCombinByte(authenByte, reserveByte);
                ackDataByte = getCombinByte(ackDataByte, mediaTypeByte);
                ackDataByte = getCombinByte(ackDataByte, pidByte);
                ackDataByte = getCombinByte(ackDataByte, ackByte);
                return ackDataByte;
            }
            case 30: {
                mediaTypeByte[0] = 30;
                byte[] pidByte = InttoBytes(requestCount);
                String requestString = "0000req=p2p_ses_mode_set&value=" + valueMode + "&streamname=" + ipPID + "_" + portPID;
                byte[] requestStringEncrypt = encrypt(requestString.getBytes(), keyEncrypt, "AES/CBC/PKCS5PADDING");
                byte[] requestStringByte = getCombinByte(authenByte, reserveByte);
                requestStringByte = getCombinByte(requestStringByte, mediaTypeByte);
                requestStringByte = getCombinByte(requestStringByte, pidByte);
                requestStringByte = getCombinByte(requestStringByte, requestStringEncrypt);

                return requestStringByte;
            }
            default:
                logger.info(appConfig.getClientId() + "outData Not exist");
                return new byte[1];
        }
    }

    public byte[] encrypt(byte[] msgByte, String key, String mode) {
        try {

            IvParameterSpec iv = new IvParameterSpec(new byte[16]);

            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance(mode);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(msgByte);
            return encrypted;
        } catch (Exception ex) {
            logger.error(appConfig.getClientId() + " error encrypt msg " + ex);
            return null;
        }

    }

    public byte[] getCombinByte(byte[] one, byte[] two) {
        byte[] combined = new byte[one.length + two.length];
        for (int i = 0; i < combined.length; ++i) {
            combined[i] = i < one.length ? one[i] : two[i - one.length];
        }
        return combined;
    }

    public boolean sendFrameMsgStreaming(DatagramSocket dataSocket, byte[] outBytes, String sip, int port, int type) {
        boolean stopStreaming = false;
        try {
            InetAddress hostAddress = InetAddress.getByName(sip);
            if (type == 34) {
                //Send message access streaming
                int countAccess = 0;
                while (true) {
                    DatagramPacket datagramPacket = new DatagramPacket(outBytes, outBytes.length, hostAddress, port);
                    dataSocket.send(datagramPacket);
                    countAccess++;
                    Thread.sleep(100);
                    if (isReceived) {
                        break;
                    }
                    if (countAccess == 100) {
                        reconnectMqtts = true;
                        logger.info(appConfig.getClientId() + " need reconnect mqtts get new session key " + reconnectMqtts);
                        break;
                    }
                }

            } else if (type == 50) {
                //Send ACK *********************************
                Thread.sleep(1000);
                int countAck = 0;
                while (true) {
                    if (isReceived) {
                        DatagramPacket datagramPacket = new DatagramPacket(outBytes, outBytes.length, hostAddress, port);
                        dataSocket.send(datagramPacket);
                        countAck++;
                        Thread.sleep(100);
                        int numAcks = appConfig.getRunTime() / 100;
                        if (countAck > numAcks) {
                            stopStreaming = true;
                            isReceived = false;
                            isSetMode = false;
                            break;
                        }

                    }
                }
            } else if (type == 30) {
                int countSetMode = 0;
                while (true) {
                    DatagramPacket datagramPacket = new DatagramPacket(outBytes, outBytes.length, hostAddress, port);
                    dataSocket.send(datagramPacket);
                    countSetMode++;
                    Thread.sleep(100);
                    if (countSetMode == 2) {
                        break;
                    }
                }
            } else {

            }
        } catch (Exception e) {
            logger.error(appConfig.getClientId() + " error when SendMsgStreaming " + e);
        }
        return stopStreaming;
    }

    public static byte[] getStringfromHex(String hex) throws UnsupportedEncodingException {
        // convert string to hexa
        int length = hex.length();
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));

        }

        return bytes;
    }

    byte[] InttoBytes(int i) {
        //convert int to byte[]
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);

        return result;
    }

    class ReceivedStreaming extends Thread {

        // receive message from streaming
        private String sip = "";
        private int port = 0;
        DatagramSocket datagramSocket;

        public ReceivedStreaming(String ipStreaming, int portStreaming, DatagramSocket dataSocket) {
            sip = ipStreaming;
            port = portStreaming;
            datagramSocket = dataSocket;
        }

        @Override
        public void run() {
            try {
                InetAddress inetAddress = InetAddress.getByName(sip);
                byte[] inDataByte = new byte[1024];
                DatagramPacket datagramPacket = new DatagramPacket(inDataByte, inDataByte.length, inetAddress, port);
                int count = 0;
                Date startRe = new Date();
                Date endRe = new Date();
                while (true) {
                    datagramSocket.receive(datagramPacket);
                    endRe = new Date();
                    isReceived = true;
                    isSetMode = true;
                    long checkRecived = endRe.getTime() - startRe.getTime();

                    if (checkRecived > appConfig.getTimeCheckStreaming()) {
                        startRe = new Date();
                        logger.info(appConfig.getClientId() + " is receiving mesage from " + appConfig.getTopicCam());
//                        logger.info("message  "+new String(datagramPacket.getData()));
                        logger.info("message length " + datagramPacket.getLength());
                    }
                    count++;
                    int byteType = datagramPacket.getData()[5];
                    if (byteType >= 10 && byteType <= 19) {
                        // get pid data audio
                        byte[] pidByte = new byte[4];
                        pidByte[0] = datagramPacket.getData()[16];
                        pidByte[1] = datagramPacket.getData()[17];
                        pidByte[2] = datagramPacket.getData()[18];
                        pidByte[3] = datagramPacket.getData()[19];
                        int x = java.nio.ByteBuffer.wrap(pidByte).getInt();
                    } else if (byteType >= 20 && byteType <= 29) {
//                        logger.info("message is video");
                    } else if (byteType == 31) {
//                        logger.info("message byte 7 " + datagramPacket.getData()[6]);
//                        logger.info("message byte 8 " + datagramPacket.getData()[7]);
//                        logger.info("message byte 9 " + datagramPacket.getData()[8]);
//                        logger.info("message byte 10 " + datagramPacket.getData()[9]);
                    }
//                    logger.info(appConfig.getAppId() + " RECEIVED ******************* ");

                }
            } catch (SocketException ex) {
                logger.info(appConfig.getClientId() + " error SocketException while received message streaming" + ex);
            } catch (IOException exception) {
                logger.info(appConfig.getClientId() + " error IOException while received message streaming " + exception);
            }
        }

        public void stopUDP() {
            datagramSocket.close();
        }

    }
}
