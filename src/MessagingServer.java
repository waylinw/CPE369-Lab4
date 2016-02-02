import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.util.IllegalFormatException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagingServer {
    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: java -cp .:\\* MessagingServer <config file path>");
            return;
        }

        Config configs = new Config();

        try {
            JSONTokener t = new JSONTokener(new FileReader(new File("config.txt")));
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

        try {
            Logger logger = Logger.getLogger("org.mongodb.driver");  // turn off logging
            logger.setLevel(Level.OFF);                              // this lets us squash a lot
            // of annoying messages

            MongoClient c = new MongoClient(configs.getMongoServer());  // connect to server
            MongoDatabase db = c.getDatabase(configs.getDatabaseName());

            MongoCollection<Document> collection = db.getCollection(configs.getCollectionName());

            FindIterable<Document> result = collection.find();

            result.forEach(new Block<Document>() {        // print each retrieved document
                @Override
                public void apply(final Document d) {
                    System.out.println(d);
                }
            });
        }
        catch(Exception e) {
            System.out.println(e);
        }
    }
}
