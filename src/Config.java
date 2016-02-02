/**
 * Created by WaylinWang on 1/30/16.
 */
public class Config {
    public String getMongoServer() {
        return mongoServer;
    }

    public void setMongoServer(String mongoServer) {
        this.mongoServer = mongoServer;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getMonitorCollectionName() {
        return monitorCollectionName;
    }

    public void setMonitorCollectionName(String monitorCollectionName) {
        this.monitorCollectionName = monitorCollectionName;
    }

    public int getDelayAmount() {
        return delayAmount;
    }

    public void setDelayAmount(int delayAmount) {
        this.delayAmount = delayAmount;
    }

    public String getWordFile() {
        return wordFile;
    }

    public void setWordFile(String wordFile) {
        this.wordFile = wordFile;
    }

    public String getClientLogFile() {
        return clientLogFile;
    }

    public void setClientLogFile(String clientLogFile) {
        this.clientLogFile = clientLogFile;
    }

    public String getServerLogFile() {
        return ServerLogFile;
    }

    public void setServerLogFile(String serverLogFile) {
        ServerLogFile = serverLogFile;
    }

    public String getQueryWordFile() {
        return queryWordFile;
    }

    public void setQueryWordFile(String queryWordFile) {
        this.queryWordFile = queryWordFile;
    }

    private String mongoServer;
    private int port;
    private String databaseName;
    private String collectionName;
    private String monitorCollectionName;
    private int delayAmount;
    private String wordFile;
    private String clientLogFile;
    private String ServerLogFile;
    private String queryWordFile;

    public Config() {
        setMongoServer("localhost");
        setPort(27017);
        setDatabaseName("test");
        setDelayAmount(10);
    }
}
