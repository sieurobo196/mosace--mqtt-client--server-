/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cinatic.lucy.cam.test.tool;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Logger;

/**
 *
 * @author NXCOMM
 */
public class MultiCamClientTestTool {

    private static final Logger logger = Logger.getLogger(MultiCamClientTestTool.class);
    String path = "./conf/cam_client_test_tool.properties";
    String pathFileCam = "./conf/multi-cam.csv";
    static String MQTT_URL = "";
    static String urlGetJson = "";
    int timeSleepSendData = 0;
    boolean isLog = false;
    long timeCheckStreaming = 0;
    List<String> listCamConf = new ArrayList<>();
    String formatCamConf = "";

    public static void main(String args[]) {

        MultiCamClientTestTool test_MQTTS = new MultiCamClientTestTool();
        test_MQTTS.RunMultiCam();
    }

    private MultiCamClientTestTool() {
        getMqttsUrl();
        listCamReadFromFileConf();
    }

    public void getMqttsUrl() {
        Properties prop = new Properties();
        InputStream input = null;

        try {

//            input = MultiCamClientTestTool.class.getClassLoader().getResourceAsStream(path);
            input = new FileInputStream(path);
            prop.load(input);
            MQTT_URL = prop.getProperty("MQTT_URL");
            formatCamConf = prop.getProperty("format_camconf");
            urlGetJson = prop.getProperty("url_get_json");
            isLog = Boolean.parseBoolean(prop.getProperty("is_log"));
            timeSleepSendData = Integer.parseInt(prop.getProperty("time_sleep_send_data"));
            timeCheckStreaming = Long.parseLong(prop.getProperty("time_check_streaming"));
            if (isLog) {
                logger.info(MQTT_URL);
                logger.info(urlGetJson);
                logger.info(timeCheckStreaming);
                logger.info(timeSleepSendData);
            }
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

            //br = new BufferedReader(new FileReader(FILENAME));
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

    public void RunMultiCam() {
        ObjectUrlJson objectUrlJson = getDataUrlJson();
        for (String camConf : listCamConf) {
            logger.info(camConf);
            CamConfig camConfig = getConfig(camConf);
            camConfig.setObjectUrlJson(objectUrlJson);
            String getSessionKey = getRegisterJson(camConfig);
            camConfig.setGetSessionKey(getSessionKey);
            camConfig.setIsLog(isLog);
            camConfig.setTimeCheckStreaming(timeCheckStreaming);
            StreamingCam startCam = new StreamingCam(camConfig);
            startCam.start();
        }

    }

    public CamConfig getConfig(String camConf) {
        CamConfig camConfig = new CamConfig();
        String clientId = camConf.split(formatCamConf)[0];
        String userName = camConf.split(formatCamConf)[1];
        String password = camConf.split(formatCamConf)[2];
        String topicCam = camConf.split(formatCamConf)[3];
        try {
            String key = "2f7d572a513f382e575c617c73744234";
            String rn = "5c43433a2a7278354f573750";
            camConfig.setMqttsUrl(MQTT_URL);
            camConfig.setClientId(clientId);
            camConfig.setKey(key);
            camConfig.setRandom(rn);
            camConfig.setUserName(userName);
            camConfig.setPassword(password);
            camConfig.setTopicCam(topicCam);
            camConfig.setTimeSleepSendData(timeSleepSendData);
            String keyEncrypt = new String(getStringfromHex(key));
            camConfig.setKeyEncrypt(keyEncrypt);
        } catch (UnsupportedEncodingException ex) {
            logger.error(clientId + " Error get keyEncrypt " + ex);
        }
        return camConfig;
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
            if (isLog) {
                logger.info(data);
            }

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

    public String getRegisterJson(CamConfig camConfig) {
        String responseMsgMqtts = "";
        String urlRegister = "https://" + camConfig.getObjectUrlJson().getSip() + "/stream/register.json";
        try {
            URL obj = new URL(urlRegister);
            HttpsURLConnection connection = (HttpsURLConnection) obj.openConnection();
            //add reuqest header
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            String urlParameters = "mac=" + camConfig.getUserName() + "&rn=" + camConfig.getRandom()
                    + "&key=" + camConfig.getKey() + "&udid=" + camConfig.getPassword();
            // Send post request
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                responseMsgMqtts = "3id: " + camConfig.getUserName() + "&time: 1496120442102&get_session_key: error=200,"
                        + "port1=" + camConfig.getObjectUrlJson().getPort1() + "&ip=" + camConfig.getObjectUrlJson().getIp()
                        + "&key=" + camConfig.getKey() + "&sip=" + camConfig.getObjectUrlJson().getSip()
                        + "&sp=" + camConfig.getObjectUrlJson().getSp() + "&rn=" + camConfig.getRandom();
            }
            if (isLog) {
                logger.info(camConfig.getClientId() + "---" + responseMsgMqtts);
            }
        } catch (IOException ex) {
            logger.info(camConfig.getClientId() + "Error IOException " + ex);
        }
        return responseMsgMqtts;
    }

    public static byte[] getStringfromHex(String hex) throws UnsupportedEncodingException {
        int length = hex.length();
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
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
}
