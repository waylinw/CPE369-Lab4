/**
 * CPE 369 - 03 Lab 4
 * Waylin Wang, Myron Zhao
 */

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerSpoof {
    static MongoClient client;
    static Config configs;
    static MongoDatabase db;
    static MongoCollection<Document> readCollection;
    static MongoCollection<Document> writeCollection;
    static FileWriter fileWriter;
    static HashMap<String, Integer> queryWords;


    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: java -cp .:\\* MessagingServer <config file path>");
            return;
        }

        setupConfigs(args[0]);

        long lastMessageCount = startupServer();
        try {
            Thread.sleep(configs.getDelayAmount() * 3000);
        }
        catch (Exception e) {
            System.out.println("Program interrupted, will continue...");
        }

        //main loop to keep server running
        boolean firstRun = true;
        boolean test = false;
        long lastMessageID = 0;

        while(test) {
            findQueryWords(1);
            try {
                Thread.sleep(configs.getDelayAmount() * 3000);
            }
            catch (Exception e){
                System.out.println("Program interrupted, will retry operation...");
            }
        }
        while(true && !test) {
            JSONObject monitorJson = getMonitorJsonStats(lastMessageCount);
            //Insert into monitor DB
            writeCollection.insertOne(Document.parse(monitorJson.toString()));
            MongoCursor<Document> lastMessage = readCollection.find().sort(new Document("_id", -1)).limit(1).iterator();

            //Print out SSR5 query results
            try {
                lastMessageCount += monitorJson.getInt("new");
                localPrintout(monitorJson);
            }
            catch (Exception e) {
                System.out.println("Cannot print result for current run... will retry in 3 second...");
            }

            //Check for query words
            if (firstRun) {
                firstRun = false;
            }
            else {
                findQueryWords(lastMessageID);
            }

            if(lastMessage.hasNext()) {
                lastMessageID = lastMessage.next().getInteger("messageID").longValue();
            }

            updateMonitorStats(monitorJson);

            try {
                Thread.sleep(configs.getDelayAmount() * 3000);
            }
            catch (Exception e){
                System.out.println("Program interrupted, will retry operation...");
            }
        }
    }

    private static void updateMonitorStats(JSONObject monitorStats) {
        UpdateOptions upsert = new UpdateOptions();
        upsert.upsert(true);

        try {
            long msgTotal = monitorStats.getLong("messages");
            long userTotal = monitorStats.getLong("users");
            long newMsgTotal = monitorStats.getLong("new");
            writeCollection.updateOne(new Document("recordType", "monitor totals"),
                    new Document("$push", new Document("msgTotals", msgTotal)
                            .append("userTotals", userTotal)
                            .append("newMsgTotals", newMsgTotal)), upsert);
        }
        catch (Exception e) {
            System.out.println("Insert to monitor DB failed. Exiting...");
            System.exit(1);
        }

    }

    /**
     * Searches for all the messages with ID after lastMsgNum for containing
     * words inside the provided queryWords.txt file
     * Then it pretty prints it out to the screen and the log file
     * @param lastMsgNum
     */
    private static void findQueryWords(long lastMsgNum) {
        for (String key: queryWords.keySet()) {
            String queryString = "\\b" + key + "\\b";
            MongoCursor<Document> queryResult = readCollection.find(
                    new Document("messageID",
                            new Document("$gt", lastMsgNum))
                            .append("text", java.util.regex.Pattern.compile(queryString))).iterator();

            if (queryResult.hasNext()) {
                JSONObject queryWordObj = new JSONObject();
                try {
                    queryWordObj.put("Word", key);
                    JSONArray mesg = new JSONArray();
                    while (queryResult.hasNext()) {
                        JSONObject temp = new JSONObject(queryResult.next().toJson());
                        mesg.put(temp);
                    }
                    queryWordObj.put("Message(s)", mesg);

                    localPrintout(queryWordObj);
                }
                catch (Exception e) {
                    System.out.println("Failed to parse JSON Object for query word: " + key);
                }
            }
        }
    }

    /**
     * Gets monitor statistics per SSR5
     * @param lastMessgCt
     * @return JSONObject with current monitor stats
     */
    private static JSONObject getMonitorJsonStats(long lastMessgCt) {
        JSONObject retVal = new JSONObject();

        //Get Time
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        //Total Message
        long messageCount = readCollection.count();

        //Unique senders
        MongoCursor<String> result = readCollection.distinct("user", String.class).iterator();
        long uniqueUsers = 0;
        while (result.hasNext()) {
            uniqueUsers++;
            result.next();
        }

        //Number of New messages
        long newMsg = messageCount - lastMessgCt;

        try {
            retVal.put("time", dateFormat.format(date).toString());
            retVal.put("messages", messageCount);
            retVal.put("users", uniqueUsers);
            retVal.put("new", newMsg);
            //Message Status Stats
            retVal.put("statusStats", getStatusInfo());
            //Recipient Status Stats
            retVal.put("recepientStats", getRecipientInfo());
        }
        catch (Exception e) {
            System.out.println("Could not make Json object for stats...Exiting");
            System.exit(1);
        }

        return retVal;
    }

    /**
     * Gets the number of public, private, all messages
     * @return JSONArray with status JSONObjects
     * @throws JSONException
     */
    private static JSONArray getStatusInfo() throws JSONException {
        JSONArray retVal = new JSONArray();

        long statusPublicCount = 0, statusPrivateCount = 0, statusProtectedCount = 0;
        MongoCursor<Document> statusPublic= readCollection.find(new Document("status", "public")).iterator();
        while (statusPublic.hasNext()) {
            statusPublicCount++;
            statusPublic.next();
        }

        MongoCursor<Document> statusPrivate= readCollection.find(new Document("status", "private")).iterator();
        while (statusPublic.hasNext()) {
            statusPrivateCount++;
            statusPrivate.next();
        }

        MongoCursor<Document> statusProtected= readCollection.find(new Document("status", "protected")).iterator();
        while (statusProtected.hasNext()) {
            statusProtectedCount++;
            statusProtected.next();
        }

        JSONObject status_pu = new JSONObject();
        status_pu.put("public", statusPublicCount);
        JSONObject status_pr = new JSONObject();
        status_pr.put("private", statusPrivateCount);
        JSONObject status_protected = new JSONObject();
        status_protected.put("protected", statusProtectedCount);
        retVal.put(status_pu);
        retVal.put(status_pr);
        retVal.put(status_protected);

        return retVal;
    }

    /**
     * Gets the number of receipients for each receipient category
     * @return JSONArray with receipient stats JSONObject
     * @throws JSONException
     */
    private static JSONArray getRecipientInfo() throws JSONException {
        JSONArray retVal = new JSONArray();

        long subscriberCount = 0, allCount = 0, selfCount = 0, userCount = 0;
        MongoCursor<Document> subscriber= readCollection.find(new Document("recepient", "subscribers")).iterator();
        while (subscriber.hasNext()) {
            subscriberCount++;
            subscriber.next();
        }

        MongoCursor<Document> all= readCollection.find(new Document("recepient", "all")).iterator();
        while (all.hasNext()) {
            allCount++;
            all.next();
        }

        MongoCursor<Document> self= readCollection.find(new Document("recepient", "self")).iterator();
        while (self.hasNext()) {
            selfCount++;
            self.next();
        }

        MongoCursor<Document> user= readCollection.find(
                new Document("recepient", java.util.regex.Pattern.compile("^u"))).iterator();
        while (user.hasNext()) {
            userCount++;
            user.next();
        }

        JSONObject subObject = new JSONObject();
        subObject.put("subscribers", subscriberCount);

        JSONObject allObject = new JSONObject();
        allObject.put("all", allCount);

        JSONObject selfObject = new JSONObject();
        selfObject.put("self", selfCount);

        JSONObject userObject = new JSONObject();
        userObject.put("userId", userCount);

        retVal.put(subObject);
        retVal.put(allObject);
        retVal.put(selfObject);
        retVal.put(userObject);

        return retVal;
    }

    /**
     * Reads in config file and stores it in a singleton
     * @param configFilePath path to config file
     */
    public static void setupConfigs(String configFilePath) {
        configs = new Config();
        try {
            JSONTokener t = new JSONTokener(new FileReader(new File(configFilePath)));
            t.skipTo('{');
            JSONObject o = new JSONObject(t);
            if (!o.isNull("mongo")) {
                configs.setMongoServer(o.getString("mongo"));
            }
            if (!o.isNull("port")) {
                configs.setPort(o.getInt("port"));
            }
            if (!o.isNull("database")){
                configs.setDatabaseName(o.getString("database"));
            }
            configs.setCollectionName(o.getString("collection"));
            configs.setMonitorCollectionName(o.getString("monitor"));
            if (configs.getCollectionName().equals(configs.getMonitorCollectionName())) {
                throw new IllegalArgumentException();
            }
            if (!o.isNull("delay")) {
                configs.setDelayAmount(o.getInt("delay"));
            }
            if (!o.isNull("words")) {
                configs.setWordFile(o.getString("words"));
            }
            if (!o.isNull("clientLog")) {
                configs.setClientLogFile(o.getString("clientLog"));
            }
            if (!o.isNull("serverLog")) {
                configs.setServerLogFile(o.getString("serverLog"));
            }
            if (!o.isNull("wordFilter")) {
                configs.setQueryWordFile(o.getString("wordFilter"));
            }
        }
        catch (IllegalFormatException k) {
            System.out.println("Collection name cannot be the same as monitor name!");
        }
        catch (Exception e) {
            System.out.println("Could not read from config file");
        }
    }

    /**
     * Start up the server as specified in SSR2
     */
    public static long startupServer() {
        Logger logger = Logger.getLogger("org.mongodb.driver");
        logger.setLevel(Level.OFF);

        try {
            client = new MongoClient(configs.getMongoServer());
        }
        catch (Exception e) {
            System.out.println("Unable to connect to: " + configs.getMongoServer());
            System.exit(0);
        }

        try {
            db = client.getDatabase(configs.getDatabaseName());
        }
        catch (Exception e) {
            System.out.println("Unable to get database: " + configs.getDatabaseName());
            System.exit(0);
        }

        try {
            readCollection = db.getCollection(configs.getCollectionName());
        }
        catch (Exception e) {
            System.out.println("Unable to open read data collection: " + configs.getCollectionName());
            System.exit(0);
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(configs.getQueryWordFile()));
            queryWords = new HashMap<String, Integer>();
            String temp;
            while ((temp = reader.readLine()) != null) {
                queryWords.put(temp, 1);
            }
        }
        catch (Exception e){
            System.out.println("Error reading from " + configs.getQueryWordFile() + ". Please try again!");
            System.exit(0);
        }

        try {
            writeCollection = db.getCollection(configs.getMonitorCollectionName());
            writeCollection.deleteMany(new Document());
            if (writeCollection.count() != 0) throw new Exception("Unable to wipe log db");
        }
        catch (Exception e) {
            System.out.println("Unable to open write data collection: " + configs.getMonitorCollectionName());
            System.exit(0);
        }

        try {
            fileWriter = new FileWriter(configs.getServerLogFile());
        }
        catch (Exception e){
            System.out.println("Could not save log to local file");
            System.exit(0);
        }



        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String currentDate = dateFormat.format(date).toString();
        long initialCount = readCollection.count();

        try {
            JSONObject obj = new JSONObject();
            obj.put("Current Time", currentDate);
            obj.put("MongoDB Connection Info", client.getConnectPoint());
            obj.put("Databse.Collection", readCollection.getNamespace());
            obj.put("Total Documents in Collection", initialCount);
            localPrintout(obj);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return initialCount;
    }

    /**
     * Writes given Json object to local log file
     * @param T
     */
    public static void localPrintout(JSONObject T) {
        try {
            System.out.println(T.toString(4));
            fileWriter.write(T.toString(4) + "\n");
            fileWriter.flush();
        }
        catch (Exception e) {
            System.out.println("Unable to write to log file");
        }
    }
}
