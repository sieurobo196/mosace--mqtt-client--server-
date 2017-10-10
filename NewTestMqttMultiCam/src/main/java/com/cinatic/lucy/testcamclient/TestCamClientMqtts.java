/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cinatic.lucy.testcamclient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
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
public class TestCamClientMqtts {
    
    private static final Logger logger = Logger.getLogger(TestCamClientMqtts.class);
    String path = "./conf/config-test-cam-client.properties";
    String pathFileCam = "./conf/multi-cam.csv";
    
    static String MQTT_URL = "";
    static List<String> listCamConf = new ArrayList<>();
    
    int sleepCam = 0;
    int runCamSync = 0;
    String urlGetJson = "";
    int count = 0;
    ObjectUrlJson objectUrlJson = new ObjectUrlJson();
    boolean cleanSession = false;
    boolean retain = false;
    int qosPub = 0;
    int qosSub = 0;
    String formatCamConf="";
    public static void main(String args[]) {
        TestCamClientMqtts test_MQTTS = new TestCamClientMqtts();
        test_MQTTS.ConnectCamAapp();
        
    }
    
    private TestCamClientMqtts() {
        getMqttsUrl();
        listCamReadFromFileConf();
    }
    
    public void getMqttsUrl() {
        
        Properties prop = new Properties();
        InputStream input = null;
        
        try {

            //input = TestCamClientMqtts.class.getClassLoader().getResourceAsStream(path);
            input = new FileInputStream(path);
            prop.load(input);
            MQTT_URL = prop.getProperty("MQTT_URL");
            urlGetJson = prop.getProperty("url_get_json");
            sleepCam = Integer.parseInt(prop.getProperty("sleep_cam"));
            runCamSync = Integer.parseInt(prop.getProperty("run_cam_sync"));
            formatCamConf=prop.getProperty("format_camconf");
            cleanSession = Boolean.parseBoolean(prop.getProperty("clean_session"));
            retain = Boolean.parseBoolean(prop.getProperty("retain"));
            qosPub = Integer.parseInt(prop.getProperty("qos_pub"));
            qosSub = Integer.parseInt(prop.getProperty("qos_sub"));
            objectUrlJson = getDataUrlJson();
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
    
    public void listCamReadFromFileConf() {
        BufferedReader br = null;
        FileReader fr = null;
        try {
            fr = new FileReader(pathFileCam);
            br = new BufferedReader(fr);
            
            String strCurrentLine;
            
            while ((strCurrentLine = br.readLine()) != null) {
                if (strCurrentLine.contains("#")) {
//                    logger.info(strCurrentLine);
                } else if (strCurrentLine.split(formatCamConf).length != 4) {
                    logger.info(strCurrentLine + " not correct format");
                } else {
                    listCamConf.add(strCurrentLine);
                }
            }
            
        } catch (IOException e) {
            logger.error(e);
            
        } finally {
            
            try {
                
                if (br != null) {
                    br.close();
                }
                
                if (fr != null) {
                    fr.close();
                }
                
            } catch (IOException ex) {
                
                logger.error(ex);
                
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
            //if (subscribeConnection == null) {
            BlockingConnection subscribeConnection = mqtt.blockingConnection();
            try {
                subscribeConnection.connect();
                logger.info("connect : " + clientId + " " + subscribeConnection.isConnected());
            } catch (Exception exception) {
                logger.error("error connect mqtts " + clientId, exception);
            }
            //}
            return subscribeConnection;
        } catch (Exception e) {
            logger.error("Error Init Mqtt Connection " + clientId, e);
            return null;
        }
        
    }
    
    public void ConnectCamAapp() {
        logger.info("start ConnectCamAapp");
        for (String camConf : listCamConf) {
            CamClient camClient = new CamClient(camConf);
            logger.info("start " + camConf);
            camClient.start();
//            if (sleepCam > 0) {
//                try {
//                    int index = listCamConf.indexOf(cam) % runCamSync;
//                    if (index == 0) {
//                        Thread.sleep(sleepCam);
//                    }
//                } catch (InterruptedException ex) {
//                    logger.info("Error Thread Cam");
//                }
//            }
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
    
    class ReceiveMessage extends Thread {
        
        BlockingConnection subConnection = null;
        String topicReceive = "";
        String clientIdRecevie = "";
        
        public ReceiveMessage(BlockingConnection blockingConnection, String topic, String userName) {
            subConnection = blockingConnection;
            topicReceive = topic;
            clientIdRecevie = userName;
        }
        
        @Override
        public void run() {
            QoS qoSSubscrice = QoS.AT_MOST_ONCE;
            if (qosSub == 1) {
                qoSSubscrice = QoS.AT_LEAST_ONCE;
            } else if (qosSub == 2) {
                qoSSubscrice = QoS.EXACTLY_ONCE;
            }
            QoS qoSPublish = QoS.AT_MOST_ONCE;
            if (qosPub == 1) {
                qoSPublish = QoS.AT_LEAST_ONCE;
            } else if (qosPub == 2) {
                qoSPublish = QoS.EXACTLY_ONCE;
            }
            Topic topic = new Topic(topicReceive, qoSSubscrice);
            Topic[] topics = {topic};
            try {
                subConnection.subscribe(topics);
//                logger.info("subscribe topic " + topicReceive);
                logger.info("wait received message from topic " + topicReceive);
                while (true) {
                    Message message = subConnection.receive();
                    String responseMsg = new String(message.getPayload());
                    logger.info("Client-" + responseMsg);
                    String timeSend = responseMsg.split("&")[1].split("=")[1];
                    String topicAppSub = responseMsg.split("&")[0].split("=")[1];
                    String msg = "3id: " + clientIdRecevie + "&time:" + timeSend + "&get_session_key: error=200,port1="
                            + objectUrlJson.getPort1() + "&ip=" + objectUrlJson.getIp() + "&key=2f7d572a513f382e575c617c73744234&sip=" + objectUrlJson.getSip() + "&sp=" + objectUrlJson.getSp() + "&rn=5c43433a2a7278354f573750";
                    subConnection.publish(topicAppSub, msg.getBytes(), qoSPublish, retain);
                    
                }
                
            } catch (Exception ex) {
                logger.error("error subscribe topic " + ex);
            }
        }
    }
    
    class CamClient extends Thread {
        
        String clientId = "";
        String userName = "";
        String password = "";
        String topicCam = "";
        
        public CamClient(String camConf) {
            clientId = camConf.split(formatCamConf)[0];
            userName = camConf.split(formatCamConf)[1];
            password = camConf.split(formatCamConf)[2];
            topicCam = camConf.split(formatCamConf)[3];
        }
        
        @Override
        public void run() {
            
            BlockingConnection connection = getConnect(clientId, userName, password);
            if (connection != null) {
                ReceiveMessage receiveMessage = new ReceiveMessage(connection, topicCam, userName);
                receiveMessage.start();
            }
        }
    }
    
    public ObjectUrlJson getDataUrlJson() {
        ObjectUrlJson json = new ObjectUrlJson();
        String data = "";
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            URL url = new URL(urlGetJson);
            URLConnection con = url.openConnection();
            Reader reader = new InputStreamReader(con.getInputStream());
            
            while (true) {
                int ch = reader.read();
                if (ch == -1) {
                    break;
                }
                
                data += (char) ch;
            }
            logger.info(data);
            
        } catch (IOException ex) {
            logger.error("error IOException" + ex);
            return null;
        } catch (NoSuchAlgorithmException ex) {
            logger.error("Error NoSuchAlgorithmException" + ex);
        } catch (KeyManagementException ex) {
            logger.error("Error KeyManagementException" + ex);
        }
        
        if (!data.isEmpty()) {
            String[] list = data.split("&");
            for (String string : list) {
                if (string.contains("ssl_port")) {
                    int port1 = Integer.parseInt(string.split("=")[1]);
                    json.setPort1(port1);
                } else if (string.contains("udpstream_port")) {
                    int sp = Integer.parseInt(string.split("=")[1]);
                    json.setSp(sp);
                } else if (string.contains("sip")) {
                    String ip = string.split("=")[1];
                    json.setIp(ip);
                } else if (string.contains("surl")) {
                    String sip = string.split("=")[1];
                    json.setSip(sip);
                }
            }
        }
        return json;
    }
    
}
