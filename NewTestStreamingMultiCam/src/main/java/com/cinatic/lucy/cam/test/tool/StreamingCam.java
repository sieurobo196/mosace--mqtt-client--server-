/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cinatic.lucy.cam.test.tool;

import com.cinatic.lucy.cam.test.tool.MultiCamClientTestTool.DefaultTrustManager;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

/**
 *
 * @author NXCOMM
 */
public class StreamingCam extends Thread {

    private static final Logger logger = Logger.getLogger(StreamingCam.class);
    private CamConfig camConfig;

    public StreamingCam(CamConfig config) {
        camConfig = config;
    }

    @Override
    public void run() {
        logger.info("Start Cam: " + camConfig.getClientId());
        BlockingConnection connection = getConnectMqtts(camConfig.getMqttsUrl(), camConfig.getUserName(), camConfig.getPassword(), camConfig.getClientId());
        ReceiveMqtts receiveMessage = new ReceiveMqtts(connection);
        receiveMessage.start();
    }

    public BlockingConnection getConnectMqtts(String MQTT_URL, String userName, String password, String clientId) {

        try {
            MQTT mqtt = new MQTT();
            mqtt.setHost(MQTT_URL);
            mqtt.setCleanSession(true);
            mqtt.setUserName(userName);
            mqtt.setPassword(password);
            mqtt.setClientId(clientId);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
            mqtt.setSslContext(sslContext);
            BlockingConnection subscribeConnection = mqtt.blockingConnection();
            try {
                subscribeConnection.connect();
                if (subscribeConnection.isConnected()) {
                    logger.info("connect : " + clientId + " success");
                } else {
                    logger.info("connect : " + clientId + " fail");
                }

            } catch (Exception exception) {
                logger.error(clientId + " error connect mqtts" + exception);
            }
            return subscribeConnection;
        } catch (Exception e) {
            logger.error(clientId + " Error Init Mqtt Connection " + e);
            return null;
        }

    }

    class ReceiveMqtts extends Thread {

        BlockingConnection subConnection = null;

        public ReceiveMqtts(BlockingConnection blockingConnection) {
            subConnection = blockingConnection;
        }

        @Override
        public void run() {
            Topic topic = new Topic(camConfig.getTopicCam(), QoS.AT_MOST_ONCE);
            Topic[] topics = {topic};
            try {
                subConnection.subscribe(topics);
                if (camConfig.isLog) {
                    logger.info(camConfig.getClientId() + " wait received msg from " + camConfig.getTopicCam());
                }
                while (true) {
                    Message message = subConnection.receive();
                    String responseMsg = new String(message.getPayload());
                    logger.info("Cam received: " + responseMsg);
                    String topicAppSub = responseMsg.split("&")[0].split("=")[1];
                    String msg = camConfig.getGetSessionKey();
                    subConnection.publish(topicAppSub, msg.getBytes(), QoS.AT_MOST_ONCE, false);
                    if (camConfig.isLog) {
                        logger.info("Send open streaming to cam ");
                    }
                    PerformStreaming();
                }

            } catch (Exception ex) {
                logger.error("error subscribe topic " + ex);
            }
        }
    }

    public void PerformStreaming() {
        try {
            String sip = camConfig.getObjectUrlJson().getSip();
            int sp = camConfig.getObjectUrlJson().getSp();
            String port1 = String.valueOf(camConfig.getObjectUrlJson().getPort1());
            String ip = camConfig.getObjectUrlJson().getIp();
            String macAddress = camConfig.getUserName();
            String key = camConfig.getKey();
            String rn = camConfig.getRandom();

            DatagramSocket datagramSocket = new DatagramSocket();
            ReceivedStreaming receivedStreaming = new ReceivedStreaming(sip, sp, datagramSocket);
            receivedStreaming.start();
            byte[] outDataOpen = createdOutData(32, macAddress, rn, key, "", 0, 0);
            sendOpenStreaming(datagramSocket, outDataOpen, sip, sp, 32);
            SendMsgStreaming msgStreaming = new SendMsgStreaming(datagramSocket, sip, sp);
            msgStreaming.start();

        } catch (SocketException ex) {
            logger.info(camConfig.getClientId() + " SocketException" + ex);
        }
    }

    class ReceivedStreaming extends Thread {

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
                    if (datagramPacket.getData()[5] == 50) {

                    } else if (datagramPacket.getData()[5] == 30) {
                        byte[] msgRequestByte = Arrays.copyOfRange(datagramPacket.getData(), 10, datagramPacket.getData().length);
                        String requestString = new String(msgRequestByte);
                        String encryptMsg = decrypt(camConfig.getKeyEncrypt(), requestString);
                        if (camConfig.isIsLog()) {
                            logger.info("message  " + new String(msgRequestByte).length());
                            logger.info("message  " + encryptMsg.length());
                        }
                    }
                    long checkRecived = endRe.getTime() - startRe.getTime();
                    if (checkRecived > camConfig.getTimeCheckStreaming()) {
                        startRe = new Date();
                        logger.info(camConfig.getClientId() + " receiving ACK***** ");

                    }
                    count++;
                }
            } catch (SocketException ex) {
                logger.error(camConfig.getClientId() + " error SocketException while received message streaming" + ex);
            } catch (IOException exception) {
                logger.error(camConfig.getClientId() + "error IOException while received message streaming " + exception);
            }
        }
    }

    public boolean sendOpenStreaming(DatagramSocket dataSocket, byte[] outBytes, String sip, int port, int type) {
        boolean sendOpen = false;
        try {
            InetAddress hostAddress = InetAddress.getByName(sip);
            if (type == 32) {
                //Send message open streaming
                int countOpen = 0;
                while (true) {
                    DatagramPacket datagramPacket = new DatagramPacket(outBytes, outBytes.length, hostAddress, port);
                    dataSocket.send(datagramPacket);
                    countOpen++;
                    Thread.sleep(100);
                    if (countOpen == 2) {
                        sendOpen = true;
                        break;
                    }

                }
            }
        } catch (Exception e) {
            logger.error(camConfig.getClientId() + " error when SendMsgStreaming " + e);
        }
        return sendOpen;
    }

    public boolean sendDataStreaming(DatagramSocket dataSocket, String sip, int port) {
        boolean sendOpen = false;
        try {
            InetAddress hostAddress = InetAddress.getByName(sip);
            int countMedia = 1;
            while (true) {
                byte[] outBytesMedia = createdOutData(13, camConfig.getUserName(), camConfig.getRandom(), camConfig.getKey(), sip, port, countMedia);
                DatagramPacket datagramPacket = new DatagramPacket(outBytesMedia, outBytesMedia.length, hostAddress, port);
                dataSocket.send(datagramPacket);
                countMedia++;
            }
        } catch (Exception e) {
            logger.error(camConfig.getClientId() + "error when SendMsgStreaming " + e);
        }
        return sendOpen;
    }

    class SendMsgStreaming extends Thread {

        DatagramSocket dataSocket;
        String sipSend;
        int portSend;

        public SendMsgStreaming(DatagramSocket datagramSocket, String ipStream, int portStream) {
            dataSocket = datagramSocket;
            sipSend = ipStream;
            portSend = portStream;

        }

        @Override
        public void run() {
            try {
                InetAddress hostAddress = InetAddress.getByName(sipSend);
                int countMedia = 1;
                while (true) {
                    byte[] outBytesMedia = createdOutData(13, camConfig.getUserName(), camConfig.getRandom(), camConfig.getKey(), sipSend, portSend, countMedia);
                    DatagramPacket datagramPacket = new DatagramPacket(outBytesMedia, outBytesMedia.length, hostAddress, portSend);
                    dataSocket.send(datagramPacket);
                    //logger.info("Send message Media: " + countMedia);
                    countMedia++;
                    if (countMedia == 1000000) {
                        countMedia = 1;
                    }
                    Thread.sleep(camConfig.getTimeSleepSendData());

                }
            } catch (Exception e) {
                logger.error(camConfig.getClientId() + " error when SendMsgStreaming " + e);
            }
        }
    }

    private String getRandomHexString(int numchars) {
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while (sb.length() < numchars) {
            if (r.nextInt() > 0) {
                sb.append(Integer.toHexString(r.nextInt()));
            }
        }

        return sb.toString().substring(0, numchars);
    }

    public byte[] createdOutData(int mediaType, String macAddress, String random, String key, String ipPID, int portPID, int numberPID) {
        String AUTH = "VLVL";
        byte[] authenByte;
        byte[] totalByte = null;
        byte[] reserveByte = new byte[1];
        byte[] mediaTypeByte = new byte[1];
        authenByte = AUTH.getBytes();
        byte[] messageAdd = new byte[985];
        for (int i = 0; i < messageAdd.length; i++) {
            messageAdd[i] = 65;
        }
        if (mediaType == 32) {
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

                mediaTypeByte[0] = 32;

                byte[] macAddressByte = macAddress.getBytes();

                try {

                    // random = getStringfromHex(random);
                    byte[] randomBumberByte = getStringfromHex(random);
                    byte[] timestampByte = getStringfromHex(Integer.toHexString(timestamp));
                    // create byte[] need encrypt
                    byte[] msgEncryptByte = getCombinByte(randomBumberByte, timestampByte);
                    String msgEncrypted = new String(msgEncryptByte);

                    try {
                        resEncryptByte = encrypt(msgEncryptByte, camConfig.getKeyEncrypt(), "AES/CBC/noPadding");

                    } catch (Exception ex) {
                        logger.error(camConfig.getClientId() + " error encrypt exception");
                    }
                } catch (UnsupportedEncodingException ex) {
                    logger.error(camConfig.getClientId() + " error exception getStringfromHex");
                }

                totalByte = getCombinByte(authenByte, reserveByte);
                totalByte = getCombinByte(totalByte, mediaTypeByte);
                totalByte = getCombinByte(totalByte, macAddressByte);
                totalByte = getCombinByte(totalByte, resEncryptByte);

            } catch (ParseException ex) {
                logger.error(camConfig.getClientId() + " ParseException " + ex);
            }
        } else if (10 <= mediaType && mediaType <= 19) {
            try {
                mediaTypeByte[0] = 13;
                TimeZone timeZone = TimeZone.getTimeZone("UTC");
                Calendar calendar = Calendar.getInstance(timeZone);
                SimpleDateFormat simpleDateFormat
                        = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
                simpleDateFormat.setTimeZone(timeZone);
                String dateString = simpleDateFormat.format(calendar.getTime());
                Date date = simpleDateFormat.parse(dateString);

                int timestamp = (int) (date.getTime() / 1000);
                byte[] timestampByte = getStringfromHex(Integer.toHexString(timestamp));
                byte[] blockLengthByte = new byte[2];
                blockLengthByte[0] = 0;
                blockLengthByte[1] = 1;
                byte[] startPIDByte = IntToByteArray(numberPID);
                byte[] thisPIDByte = IntToByteArray(numberPID);
                String msg = "hello messasge";
                byte[] msgByte = msg.getBytes();
                totalByte = getCombinByte(authenByte, reserveByte);
                totalByte = getCombinByte(totalByte, mediaTypeByte);
                totalByte = getCombinByte(totalByte, timestampByte);
                totalByte = getCombinByte(totalByte, blockLengthByte);
                totalByte = getCombinByte(totalByte, startPIDByte);
                totalByte = getCombinByte(totalByte, thisPIDByte);
                totalByte = getCombinByte(totalByte, msgByte);
                totalByte = getCombinByte(totalByte, messageAdd);

            } catch (ParseException ex) {
                logger.error(camConfig.getClientId() + " ParseException " + ex);
            } catch (UnsupportedEncodingException ex) {
                logger.error(camConfig.getClientId() + " UnsupportedEncodingException " + ex);
            }
        }
        return totalByte;
    }

    public byte[] getCombinByte(byte[] one, byte[] two) {
        byte[] combined = new byte[one.length + two.length];
        for (int i = 0; i < combined.length; ++i) {
            combined[i] = i < one.length ? one[i] : two[i - one.length];
        }
        return combined;
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
            logger.info(camConfig.getClientId() + " error encrypt msg " + ex.getMessage());
            return null;
        }

    }

    public static byte[] getStringfromHex(String hex) throws UnsupportedEncodingException {
        int length = hex.length();
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    public byte[] IntToByteArray(int i) {
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);

        return result;
    }

    public String decrypt(String key, String encrypted) {
        try {

            IvParameterSpec iv = new IvParameterSpec(new byte[16]);
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

            return new String(original);
        } catch (Exception ex) {
            logger.error(camConfig.getClientId() + " Exception");
        }

        return null;
    }

}
