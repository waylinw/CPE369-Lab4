import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by WaylinWang on 1/28/16.
 */

public class MessagingServer {
    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: java -cp .:\\* MessagingServer <config file path>");
            return;
        }

        JSONParser parser = new JSONParser();

        try {

            Object obj = parser.parse(new FileReader(
                    "/Users/<username>/Documents/file1.txt"));

            JSONObject jsonObject = (JSONObject) obj;
        Config serverConfig = new Config();
        try {
            t = new JSONTokener(new FileInputStream(args[0]));
            while (t.skipTo('{') == 0){};
            JSONObject obj = new JSONObject(t);
            serverConfig.setMongoServer(obj.getString("mongo"));
        } catch (Exception e) {
            System.out.println("Opening Json file failed, please make sure path is correct.");
            return;
        }





        try {
            Logger logger = Logger.getLogger("org.mongodb.driver");  // turn off logging
            logger.setLevel(Level.OFF);                              // this lets us squash a lot
            // of annoying messages

            MongoClient c = new MongoClient("cslvm31");  // connect to server
            MongoDatabase db = c.getDatabase("wwang16");

            MongoCollection<Document> collection = db.getCollection("Data");

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
