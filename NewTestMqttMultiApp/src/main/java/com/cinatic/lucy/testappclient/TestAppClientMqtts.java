/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cinatic.lucy.testappclient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
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
 * @author NXCOMM
 */
public class TestAppClientMqtts {

    private static final Logger logger = Logger.getLogger(TestAppClientMqtts.class);
    String path = "./conf/config-test-app-client.properties";
    String pathFileApp = "./conf/multi-app.csv";
    static String MQTT_URL = "";

    static List<String> listAppConf = new ArrayList<>();
    int timeoutSend = 0;

    int sleepApp = 0;
    int runAppSync = 0;
    boolean isSendMsg = false;
    int timeRunApp = 0;

    boolean cleanSession = false;
    boolean retain = false;
    int qosPub = 0;
    int qosSub = 0;
    int numberSendMsgPerSeCond = 0;
    String formatAppConf = "";

    public static void main(String args[]) {
        TestAppClientMqtts test_MQTTS = new TestAppClientMqtts();
        test_MQTTS.ConnectCamAapp();
    }

    private TestAppClientMqtts() {
        getMqttsUrl();
        listAppReadFromFileConf();
    }

    public void getMqttsUrl() {
        Properties prop = new Properties();
        InputStream input = null;

        try {

//            input = TestAppClientMqtts.class.getClassLoader().getResourceAsStream(path);
            input = new FileInputStream(path);
            prop.load(input);
            MQTT_URL = prop.getProperty("MQTT_URL");
            logger.info(prop.getProperty("MQTT_URL"));
            formatAppConf = prop.getProperty("format_appconf");
            isSendMsg = Boolean.parseBoolean(prop.getProperty("send_message"));
            timeoutSend = Integer.parseInt(prop.getProperty("timeout_send"));
            sleepApp = Integer.parseInt(prop.getProperty("sleep_app"));
            runAppSync = Integer.parseInt(prop.getProperty("run_app_sync"));
            timeRunApp = Integer.parseInt(prop.getProperty("time_run_app"));
            cleanSession = Boolean.parseBoolean(prop.getProperty("clean_session"));
            retain = Boolean.parseBoolean(prop.getProperty("retain"));
            qosPub = Integer.parseInt(prop.getProperty("qos_pub"));
            qosSub = Integer.parseInt(prop.getProperty("qos_sub"));
            numberSendMsgPerSeCond = Integer.parseInt(prop.getProperty("number_send_msg_per_second"));

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void listAppReadFromFileConf() {
        BufferedReader br = null;
        FileReader fr = null;

        try {

            //br = new BufferedReader(new FileReader(FILENAME));
            fr = new FileReader(pathFileApp);
            br = new BufferedReader(fr);

            String strCurrentLine;

            while ((strCurrentLine = br.readLine()) != null) {
                if (strCurrentLine.contains("#")) {
//                    logger.info(strCurrentLine);
                } else if (strCurrentLine.split(formatAppConf).length != 5) {
                    logger.info(strCurrentLine + " not correct format");
                } else {
                    listAppConf.add(strCurrentLine);
                }
            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (br != null) {
                    br.close();
                }

                if (fr != null) {
                    fr.close();
                }

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }
    }

    public BlockingConnection getConnect(String clientId, String userName, String password) {
        try {
            MQTT mqtt = new MQTT();
            mqtt.setHost(MQTT_URL);
            mqtt.setCleanSession(cleanSession);
            mqtt.setUserName(userName);
            mqtt.setPassword(password);
            mqtt.setClientId(clientId);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
            mqtt.setSslContext(sslContext);
            BlockingConnection subscribeConnection = mqtt.blockingConnection();
            try {
                subscribeConnection.connect();
                logger.info("connect : " + clientId + " " + subscribeConnection.isConnected());

            } catch (Exception exception) {
                logger.error("error connect mqtts" + exception);
            }

            return subscribeConnection;
        } catch (Exception e) {
            logger.error("Error Init Mqtt Connection " + e);
            return null;
        }

    }

    public void ConnectCamAapp() {
        logger.info("start ConnectCamAapp");
        for (String appclient : listAppConf) {
            logger.info(appclient);
            AppClient appClient = new AppClient(appclient);
            appClient.start();
        }
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

    class PublicMessage extends Thread {

        BlockingConnection subConnection = null;
        String topicCam = "";
        String clientApp = "";
        String topicApp = "";
        AppClient appClient;

        public PublicMessage(AppClient app, BlockingConnection blockingConnection, String topicCamPub, String clientId, String topicAppSub) {
            subConnection = blockingConnection;
            topicCam = topicCamPub;
            clientApp = clientId;
            topicApp = topicAppSub;
            appClient = app;
        }

        @Override
        public void run() {
            int count = 0;
            try {
                long startTime = System.currentTimeMillis();

                while (true) {

                    long elapseTime = System.currentTimeMillis() - startTime;
                    if (elapseTime > timeRunApp) {
                        logger.info(clientApp + " stop send message");

                        break;
                    }
                    String msg = "";
                    msg = "2app_topic_sub=" + topicApp + "&time=" + System.currentTimeMillis() + "&req=get_session_key&mode=remote&port1=50915&ip=115.78.13.114&streamname=CC3A61D7DEC6_50915";

                    QoS qoSPublish = QoS.AT_MOST_ONCE;
                    if (qosPub == 1) {
                        qoSPublish = QoS.AT_LEAST_ONCE;
                    } else if (qosPub == 2) {
                        qoSPublish = QoS.EXACTLY_ONCE;
                    }
                    byte[] byteMsg = msg.getBytes();
                    long startSendMsg = System.currentTimeMillis();
                    for (int i = 0; i < numberSendMsgPerSeCond; i++) {
                        subConnection.publish(topicCam, byteMsg, qoSPublish, retain);
                        count++;
                    }
                    long sendMsgTime = System.currentTimeMillis() - startSendMsg;
                    logger.info("publish msg " + sendMsgTime);

                    if (sendMsgTime < 1000) {
                        logger.info(msg);
                        Thread.sleep(1000 - sendMsgTime);
                    }

                }
            } catch (Exception ex) {

                logger.error("error publish messsage " + ex);
            }
            logger.info(clientApp + " total send: " + count);

        }
    }

    class ReceiveMessage extends Thread {

        BlockingConnection subConnection = null;
        String topicSub = "";
        String clientIdRecevie = "";
        AppClient app;
        long startTime;

        public ReceiveMessage(AppClient app, BlockingConnection blockingConnection, String topicApp, String clientId) {
            subConnection = blockingConnection;
            clientIdRecevie = clientId;
            topicSub = topicApp;
            this.app = app;
        }

        @Override
        public void run() {
            QoS qoSSubscrice = QoS.AT_MOST_ONCE;
            if (qosSub == 1) {
                qoSSubscrice = QoS.AT_LEAST_ONCE;
            } else if (qosSub == 2) {
                qoSSubscrice = QoS.EXACTLY_ONCE;
            }
            Topic topic = new Topic(topicSub, qoSSubscrice);
            Topic[] topics = {topic};
            try {
                subConnection.subscribe(topics);
                logger.info("subscribe topic " + topicSub);
                logger.info("wait received message from " + topicSub);
                while (true) {
                    Message message = subConnection.receive(5, TimeUnit.SECONDS);
                    String responseMsg = new String(message.getPayload());
                    logger.info(responseMsg);
                    long startSendTime = 0;
                    startSendTime = Long.parseLong(responseMsg.split("&")[1].split(":")[1].replaceAll("\\s", ""));
                    long latency = System.currentTimeMillis() - startSendTime;
                    app.totalLatency += latency;
                    app.totalReceive++;
                    logger.info("LATENCY SEND-RECEIVED: " + latency + " ms");
                }

            } catch (Exception ex) {
                logger.error("error timeout received " + ex);
            }
            logger.info(clientIdRecevie + " total receive: " + app.totalReceive);
            logger.info(clientIdRecevie + " total latency: " + app.totalLatency);
            logger.info(clientIdRecevie + " average latency: " + (app.totalReceive == 0 ? "NO RECEIVE" : ("" + (app.totalLatency / app.totalReceive))));
        }
    }

    class AppClient extends Thread {

        String clientId = "";
        String userName = "";
        String password = "";
        String topicApp = "";
        String topicCam = "";
        public long totalLatency = 0;
        public int totalReceive = 0;

        public AppClient(String appClient) {
            clientId = appClient.split(formatAppConf)[0];
            userName = appClient.split(formatAppConf)[1];
            password = appClient.split(formatAppConf)[2];
            topicApp = appClient.split(formatAppConf)[3];
            topicCam = appClient.split(formatAppConf)[4];
        }

        @Override
        public void run() {

            BlockingConnection connection = getConnect(clientId, userName, password);
            ReceiveMessage receiveMessage = new ReceiveMessage(this, connection, topicApp, clientId);
            receiveMessage.start();
            if (isSendMsg) {
                PublicMessage publicMessage = new PublicMessage(this, connection, topicCam, clientId, topicApp);
                publicMessage.start();
            }

        }
    }
}
