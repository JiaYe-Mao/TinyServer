package org.mao.tinyserver.config;

import org.mao.tinyserver.exception.InvalidConfigException;
import org.mao.tinyserver.utils.LoggerUtil;
import org.mao.tinyserver.utils.XMLUtil;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerConfigBuilder {

    private static final Logger logger = LoggerUtil.getLogger(ServerConfigBuilder.class);
    public static final String DEFAULT_CONFIG_FILE = "src/main/resources/ServerConfig.xml";
    public static final char SEPARATOR_CHAR = '/';

    private Document document;

    public ServerConfigBuilder(){
        this.document = findConfigXML(new File(DEFAULT_CONFIG_FILE));
    }

    public ServerConfigBuilder(String URL){
        this.document = findConfigXML(new File(URL));
    }

    public ServerConfig build() {

        ServerConfig serverConfig = new ServerConfig();

        //todo:实现地比较蠢,准备搞个自动化匹配
        Integer port = null;
        try {
            port = Integer.parseInt(XMLUtil.findContentByURL("/serverconfig/port", document));
            serverConfig.setPort(port);
        } catch (XPathExpressionException | NullPointerException e) {
            logger.log(Level.INFO, "cannot find port in serverConfig, set port to 8888");
        }
        Integer maxThread = null;
        try {
            maxThread = Integer.parseInt(XMLUtil.findContentByURL("/serverconfig/maxthread", document));
            if (maxThread < 1 || maxThread > 1000){
                throw new InvalidConfigException("maxThread need to be less than 1000, larger than 0");
            }
            serverConfig.setMaxThread(maxThread);
        } catch (XPathExpressionException | NullPointerException e) {
            logger.log(Level.INFO, "cannot find maxThread in serverConfig, set maxThread to 50");
        }
        String staticURLPath = null;
        try {
            staticURLPath = XMLUtil.findContentByURL("/serverconfig/staticroot/urlpath", document);
            if (staticURLPath.endsWith("/")){
                staticURLPath = staticURLPath.substring(0, staticURLPath.length()-1);
            }
            serverConfig.setStaticURLPath(staticURLPath);
        } catch (XPathExpressionException | NullPointerException e) {
            logger.log(Level.SEVERE, "cannot find staticUrlPath in serverConfig");
            throw new RuntimeException();
        }
        String staticLocationPath = null;
        try {
            staticLocationPath = XMLUtil.findContentByURL("/serverconfig/staticroot/locationpath", document);
            if (staticLocationPath.endsWith("/")){
                staticLocationPath = staticLocationPath.substring(0, staticLocationPath.length()-1);
            }
            serverConfig.setStaticLocationPath(staticLocationPath);
        } catch (XPathExpressionException | NullPointerException e) {
            logger.log(Level.SEVERE, "cannot find staticLocationPath in serverConfig");
            throw new RuntimeException();
        }
        String welcomeFile = null;
        try {
            welcomeFile = XMLUtil.findContentByURL("/serverconfig/welcomefile", document);
            serverConfig.setWelcomeFile(welcomeFile);
        } catch (XPathExpressionException | NullPointerException e) {
            logger.log(Level.SEVERE, "cannot find welcomeFile in serverConfig");
            throw new RuntimeException();
        }
        String serviceRoot = null;
        try {
            serviceRoot = XMLUtil.findContentByURL("/serverconfig/serviceroot", document);
            serverConfig.setServiceRoot(serviceRoot);
        } catch (XPathExpressionException | NullPointerException e) {
            logger.log(Level.SEVERE, "cannot find serviceRoot in serverConfig");
            throw new RuntimeException();
        }
        String openFileManager = null;
        try {
            openFileManager = XMLUtil.findContentByURL("/serverconfig/filemanager", document);
            if ("true".equals(openFileManager)){
                serverConfig.setFileManager(true);
            }
        } catch (XPathExpressionException | NullPointerException e) {
            logger.log(Level.INFO, "cannot find filemanager in config, set isFileManager to false");
        }
        String sslKeyStore = null;
        try {
            sslKeyStore = XMLUtil.findContentByURL("/serverconfig/ssl/sslkeystore", document);
            serverConfig.setSslKeyStore(sslKeyStore);
        } catch (XPathExpressionException | NullPointerException e) {
            logger.log(Level.INFO, "cannot find sslKeyStore in config, not supporting HTTPS");
        }
        String sslPassWord = null;
        try {
            sslPassWord = XMLUtil.findContentByURL("/serverconfig/ssl/sslpassword", document);
            serverConfig.setSslPassWord(sslPassWord);
        } catch (XPathExpressionException | NullPointerException e) {
            logger.log(Level.INFO, "cannot find filemanager in config, not supporting HTTPS");
        }
        Integer maxRequestKb = null;
        try {
            maxRequestKb = Integer.parseInt(XMLUtil.findContentByURL("/serverconfig/maxrequestkb", document));
            serverConfig.setMaxRequestKb(maxRequestKb);
        } catch (XPathExpressionException | NullPointerException e) {
            logger.log(Level.INFO, "cannot find maxRequestKb in serverConfig, set maxRequestKb to 2048");
        }
        Integer corePoolSize = null;
        try {
            corePoolSize = Integer.parseInt(XMLUtil.findContentByURL("/serverconfig/corepoolsize", document));
            serverConfig.setCorePoolSize(corePoolSize);
        } catch (XPathExpressionException | NullPointerException e) {
            logger.log(Level.INFO, "cannot find corePoolSize in serverConfig, set corePoolSize to 50");
        }

        return serverConfig;

    }

    private Document findConfigXML(File file){
        Document document = null;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.parse(file);
        } catch (Exception e){
            logger.log(Level.SEVERE, "Cannot find serverConfig.xml");
            System.exit(1);
        }
        return document;
    }
}
