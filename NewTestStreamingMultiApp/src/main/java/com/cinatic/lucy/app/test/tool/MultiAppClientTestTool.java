/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cinatic.lucy.app.test.tool;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.Logger;

/**
 *
 * @author cu.tran
 */
public class MultiAppClientTestTool {

    private static final Logger logger = Logger.getLogger(MultiAppClientTestTool.class);
    String path = "./conf/app_client_test_tool.properties";
    String pathFileApp = "./conf/multi-app.csv";
    static String MQTT_URL = "";
    static int runTime = 0;
    static int restTime = 0;
    List<String> listAppConf = new ArrayList<>();

    boolean isLog = false;
    long timeCheckStreaming = 0;
    String formatAppConf = "";

    public static void main(String args[]) {
        MultiAppClientTestTool mqttCamTest = new MultiAppClientTestTool();
        mqttCamTest.RunMultiApp();

    }

    private MultiAppClientTestTool() {
        getMqttsUrl();
        listAppReadFromFileConf();
    }

    public void getMqttsUrl() {
        Properties prop = new Properties();
        InputStream input = null;

        try {

//            input = MultiAppClientTestTool.class.getClassLoader().getResourceAsStream(path);
            input = new FileInputStream(path);
            prop.load(input);
            MQTT_URL = prop.getProperty("MQTT_URL");
            runTime = Integer.parseInt(prop.getProperty("run_time"));
            restTime = Integer.parseInt(prop.getProperty("rest_time"));
            formatAppConf = prop.getProperty("format_appconf");
            timeCheckStreaming = Long.parseLong(prop.getProperty("time_check_streaming"));
            isLog = Boolean.parseBoolean(prop.getProperty("is_log"));
            if (isLog) {
                logger.info(MQTT_URL);
                logger.info(runTime);
                logger.info(restTime);
                logger.info(timeCheckStreaming);
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

    public void RunMultiApp() {
        for (String appConf : listAppConf) {
            String clientId =appConf.split(formatAppConf)[0];
            String userName =appConf.split(formatAppConf)[1];
            String password =appConf.split(formatAppConf)[2];
            String topicApp =appConf.split(formatAppConf)[3];
            String topicCam =appConf.split(formatAppConf)[4];
            AppConfig appConfig = new AppConfig();
            appConfig.setMqttsUrl(MQTT_URL);
            appConfig.setClientId(clientId);
            appConfig.setUserName(userName);
            appConfig.setPassword(password);
            appConfig.setTopicApp(topicApp);
            appConfig.setTopicCam(topicCam);
            appConfig.setRunTime(runTime);
            appConfig.setRestTime(restTime);
            appConfig.setIsLog(isLog);
            appConfig.setTimeCheckStreaming(timeCheckStreaming);
            StreamingApp streamingApp = new StreamingApp(appConfig);
            streamingApp.start();
        }
    }


}
