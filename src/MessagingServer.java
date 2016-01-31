import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by WaylinWang on 1/28/16.
 */

public class MessagingServer {
    public static void main(String[] args) {



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
