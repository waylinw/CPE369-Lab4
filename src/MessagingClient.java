/**
 * Created by WaylinWang on 1/28/16.
 */

import java.io.*;
import java.util.Calendar;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.*;
import com.mongodb.MongoClient;

public class MessagingClient {
    public static void main(String[] args) throws JSONException{
        InputStream inputStream;
        JSONTokener tokener;
        JSONObject jsonObject;
        String configFile;
        Configuration config = new Configuration();
        Calendar calendar = Calendar.getInstance();

        if (args.length < 1) {
            System.out.println("Error: No configuration file. Please try again.");
            return;
        }

        try {
            configFile = args[0];
            inputStream = new FileInputStream(configFile);
            tokener = new JSONTokener(inputStream);

        } catch (Exception e) {
            System.out.println("File not found. Please try again with a valid file name!");
            return;
        }

        tokener.skipTo('{');
        jsonObject = new JSONObject(tokener);

        if (jsonObject.getString("mongo") != null || jsonObject.getString("mongo") != "") {
            config.MongoServer = "localhost";
        } else {
            config.MongoServer = jsonObject.getString("mongo");
        }

        if (jsonObject.get("port") != null) {
            config.MongoPort = jsonObject.getInt("port");
        } else {
            config.MongoPort = 27017;
        }

        if (jsonObject.getString("database") != null) {
            config.DBName = jsonObject.getString("database");
        } else {
            config.DBName = "test";
        }

        config.CollectionName = jsonObject.getString("collection");
        config.MonitorCollName = jsonObject.getString("monitor");

        if (config.CollectionName.equals(config.MonitorCollName)) {
            System.out.println("Error: Monitor collection name must be different from collection name. Please try " +
                    "again with a valid configuration file.");
        }

        if (jsonObject.get("delay") == null || jsonObject.getInt("delay") == 0) {
            config.delayAmount = 10;
        } else {
            config.delayAmount = jsonObject.getInt("delay");
        }

        config.wordFile = jsonObject.getString("words");
        config.clientLogFile = jsonObject.getString("clientLog");
        config.serverLogFile = jsonObject.getString("serverLogFile");
        config.queryWordFile = jsonObject.getString("queryWordFile");

        MongoClient client = new MongoClient(config.MongoServer, config.MongoPort);
        final MongoDatabase db = client.getDatabase(config.DBName);
        MongoCollection<Document> cl = db.getCollection(config.CollectionName);

        System.out.println("Current timestamp: " + calendar.getTime());
        System.out.println("Mongo server: " + config.MongoServer);
        System.out.println("Port: " + config.MongoPort);
        System.out.println("Database name: " + config.DBName);
        System.out.println("Collection name: " + config.CollectionName);
        System.out.println("Number of documents in collection: " + cl.count());


    }

    public static class Configuration {
        String MongoServer, DBName, CollectionName, MonitorCollName, wordFile, clientLogFile, serverLogFile,
                queryWordFile;
        Integer MongoPort, delayAmount;

        public Configuration() {
            ;
        }
    }
}
