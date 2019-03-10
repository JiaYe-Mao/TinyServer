package org.mao.tinyserver.config;

public class ServerConfig {

    private Integer port = 8888;
    private Integer maxThread = 200;
    private Integer corePoolSize = 50;
    private String staticURLPath;
    private String staticLocationPath;
    private String welcomeFile;
    private String serviceRoot;
    private Integer maxRequestKb = 2048;          // 防止Post的文件过大, 把内存挤爆
    private String characterEncoding = "UTF-8";
    private String SERVER_INFO = "TinyServer/1.0";
    private boolean FileManager = false;

    // todo:implememts
    private boolean cache = true;

    private String sslKeyStore;
    private String sslPassWord;

    // ip
    // MaxRequestBytes       默认:2048     写config
    // characterEncoding
    // keepaliveTimeOut


    public boolean isCache() {
        return cache;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }

    public String getSslKeyStore() {
        return sslKeyStore;
    }

    public void setSslKeyStore(String sslKeyStore) {
        this.sslKeyStore = sslKeyStore;
    }

    public String getSslPassWord() {
        return sslPassWord;
    }

    public void setSslPassWord(String sslPassWord) {
        this.sslPassWord = sslPassWord;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxThread() {
        return maxThread;
    }

    public void setMaxThread(int maxThread) {
        this.maxThread = maxThread;
    }

    public String getStaticURLPath() {
        return staticURLPath;
    }

    public void setStaticURLPath(String staticURLPath) {
        this.staticURLPath = staticURLPath;
    }

    public String getStaticLocationPath() {
        return staticLocationPath;
    }

    public void setStaticLocationPath(String staticLocationPath) {
        this.staticLocationPath = staticLocationPath;
    }

    public String getWelcomeFile() {
        return welcomeFile;
    }

    public void setWelcomeFile(String welcomeFile) {
        this.welcomeFile = welcomeFile;
    }

    public String getServiceRoot() {
        return serviceRoot;
    }

    public void setServiceRoot(String serviceRoot) {
        this.serviceRoot = serviceRoot;
    }

    public Integer getMaxRequestKb() {
        return maxRequestKb;
    }

    public void setMaxRequestKb(Integer maxRequestKb) {
        this.maxRequestKb = maxRequestKb;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public String getSERVER_INFO() {
        return SERVER_INFO;
    }

    public void setSERVER_INFO(String SERVER_INFO) {
        this.SERVER_INFO = SERVER_INFO;
    }


    public boolean isFileManager() {
        return FileManager;
    }

    public void setFileManager(boolean fileManager) {
        FileManager = fileManager;
    }

    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "port=" + port +
                ", maxThread=" + maxThread +
                ", staticURLPath='" + staticURLPath + '\'' +
                ", staticLocationPath='" + staticLocationPath + '\'' +
                ", welcomeFile='" + welcomeFile + '\'' +
                ", serviceRoot='" + serviceRoot + '\'' +
                ", maxRequestKb=" + maxRequestKb +
                ", characterEncoding='" + characterEncoding + '\'' +
                ", SERVER_INFO='" + SERVER_INFO + '\'' +
                ", FileManager=" + FileManager +
                '}';
    }
}
