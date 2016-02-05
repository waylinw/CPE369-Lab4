/**
 * CPE 369 - 03 Lab 4
 * Waylin Wang, Myron Zhao
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.json.*;
import com.mongodb.MongoClient;

public class ClientSpoof {
    public static void main(String[] args) {
        final int millis = 1000;
        final int maxCount = 40;

        JSONObject jsonObject;
        JSONTokener tokener;

        MongoClient client = null;
        MongoCollection<Document> cl = null;

        BufferedReader reader;
        FileWriter fileWriter;
        InputStream inputStream;

        ArrayList<String> lines = new ArrayList<>();
        Configuration config = new Configuration();
        Message msg = new Message();
        Random rng = new Random();

        Calendar calendar;
        String configFile;
        String lastUser = "";
        String line;

        double actualDelay = 0;
        int curMsg = 1;
        int cycleCount = 1;
        int lastUserMessages = 0;

        // Check for command line arguments
        if (args.length < 1) {
            System.out.println("Error: No configuration file. Please try again.");
            return;
        }

        // Create an input stream if config file is given
        try {
            configFile = args[0];
            inputStream = new FileInputStream(configFile);
            tokener = new JSONTokener(inputStream);

        } catch (Exception e) {
            System.out.println("File not found. Please try again with a valid file name!");
            return;
        }

        // Parse config file for JSON object
        try {
            tokener.skipTo('{');
            jsonObject = new JSONObject(tokener);

            if (jsonObject.getString("mongo") == null || jsonObject.getString("mongo") == "") {
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
                System.out.println("Error: Monitor collection name must be different from collection name. Please " +
                        "try again with a valid configuration file.");
            }

            if (jsonObject.get("delay") == null || jsonObject.getInt("delay") == 0) {
                config.delayAmount = 10;
            } else {
                config.delayAmount = jsonObject.getInt("delay");
            }

            config.wordFile = jsonObject.getString("words");
            config.clientLogFile = jsonObject.getString("clientLog");
            config.serverLogFile = jsonObject.getString("serverLog");
            config.queryWordFile = jsonObject.getString("wordFilter");
        } catch (JSONException e) {
            System.out.println("Error: JSONException.");
        }

        // Set up connection to MongoDB server
        try {
            client = new MongoClient(config.MongoServer, config.MongoPort);
            final MongoDatabase db = client.getDatabase(config.DBName);
            cl = db.getCollection(config.CollectionName);
        } catch (Exception e) {
            System.out.println("Error: Could not connect to Mongo server.");
        }

        //Read lines from input file
        try {
            reader = new BufferedReader(new FileReader(config.wordFile));
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        catch (Exception e){
            System.out.println("Error reading from " + config.wordFile + ". Please try again!");
            e.printStackTrace();
            return;
        }

        //Setup file output writer
        try {
            fileWriter = new FileWriter(config.clientLogFile);
        }
        catch (Exception e) {
            System.out.println("Create file failed. Please try again!");
            return;
        }

        // Print startup diagnostics
        calendar = Calendar.getInstance();
        System.out.println("Current timestamp: " + calendar.getTime());
        System.out.println("Mongo server: " + config.MongoServer);
        System.out.println("Port: " + config.MongoPort);
        System.out.println("Database name: " + config.DBName);
        System.out.println("Collection name: " + config.CollectionName);
        System.out.println("Number of documents in collection: " + cl.count());

        try {
            fileWriter.write("Current timestamp: " + calendar.getTime());
            fileWriter.write("Mongo server: " + config.MongoServer);
            fileWriter.write("Port: " + config.MongoPort);
            fileWriter.write("Database name: " + config.DBName);
            fileWriter.write("Collection name: " + config.CollectionName);
            fileWriter.write("Number of documents in collection: " + cl.count());
        } catch (IOException e) {
            System.out.println("Error: Could not write to " + config.clientLogFile + ". Please try again.");
        }

        // Start main forever loop
        for (;;) {
            if (cycleCount == maxCount) {
                cycleCount = 1;

                FindIterable findIterable = cl.find(Filters.eq("user", lastUser));
                MongoCursor mongoCursor = findIterable.iterator();

                while (mongoCursor.hasNext()) {
                    lastUserMessages++;
                    mongoCursor.next();
                }

                System.out.println("Number of messages in collection: " + cl.count());
                System.out.println("Number of messages written by last author: " + lastUserMessages);
                try {
                    fileWriter.write("Number of documents in collection: " + cl.count());
                    fileWriter.write("Number of messages written by last author: " + lastUserMessages);
                } catch (IOException e) {
                    System.out.println("Error: Could not write to " + config.clientLogFile + ". Please try again.");
                }

                lastUserMessages = 0;
            } else {
                cycleCount++;
            }

            // Randomly generate a delay amount
            actualDelay = rng.nextGaussian() * (config.delayAmount/2) + config.delayAmount;
            if (actualDelay < 2) {
                actualDelay = 2;
            } else if (actualDelay > 4 * config.delayAmount) {
                actualDelay = 4 * config.delayAmount;
            }

            // Sleep for delay amount
            try {
                Thread.sleep((long) actualDelay * millis);
            } catch (InterruptedException e) {
                System.out.println("Error: Sleep interrupted.");
            }

            // Create a JSON object to send to collection
            curMsg += rng.nextInt(5) + 1;
            msg.genMessage(curMsg, rng, lines);
            lastUser = msg.getUser();

            try {
                //Determine if message is in-response and create JSON object accordingly
                if (msg.getInResponse() != -1) {
                    jsonObject = new JSONObject()
                            .put("messageID", msg.getMessageId())
                            .put("user", msg.getUser())
                            .put("status", msg.getStatus())
                            .put("recepient", msg.getRecepient())
                            .put("in-response", msg.getInResponse())
                            .put("text", msg.getText());
                } else {
                    jsonObject = new JSONObject()
                            .put("messageID", msg.getMessageId())
                            .put("user", msg.getUser())
                            .put("status", msg.getStatus())
                            .put("recepient", msg.getRecepient())
                            .put("text", msg.getText());
                }

                // Print message and timestamp
                calendar = Calendar.getInstance();
                System.out.println("Current timestamp: " + calendar.getTime());
                System.out.println(jsonObject.toString(1));
                cl.insertOne(Document.parse(jsonObject.toString(1)));

                try {
                    fileWriter.write("Current timestamp: " + calendar.getTime());
                    fileWriter.write(jsonObject.toString(1));
                } catch (IOException e) {
                    System.out.println("Error: Could not write to " + config.clientLogFile + ". Please try again.");
                }
            } catch (JSONException e) {
                System.out.println("Error: JSONException.");
            }


        }

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

class Message {

    private int messageId, inResponse;
    private String user, status, recepient, text;

    public Message() {
    }

    public Message(int messageId, int inResponse, String user, String status, String recepient, String text) {
        this.messageId = messageId;
        this.inResponse = inResponse;
        this.user = user;
        this.status = status;
        this.recepient = recepient;
        this.text = text;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getInResponse() {
        return inResponse;
    }

    public void setInResponse(int inResponse) {
        this.inResponse = inResponse;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecepient() {
        return recepient;
    }

    public String setRecepient(String recepient) {
        this.recepient = recepient;
        return this.recepient;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void genMessage (int curMsg, Random rng, ArrayList<String> lines) {
        int rngNum;
        int numWords;
        String line = "";
        this.setMessageId(curMsg);
        this.setUser("u" + (rng.nextInt(10000) + 1));

        //Randomly set a status type
        if ((rngNum = rng.nextInt(100)) < 80) {
            this.setStatus("public");
        }
        else if (rngNum < 90) {
            this.setStatus("private");
        }
        else {
            this.setStatus("protected");
        }

        //Randomly generate recepient for public statuses
        if (this.getStatus().equals("public")) {
            if ((rngNum = rng.nextInt(100)) < 40) {
                this.setRecepient("all");
            }
            else if (rngNum < 80) {
                this.setRecepient("subscribers");
            }
            else if (rngNum < 95) {
                this.setRecepient("u" + (rng.nextInt(10000) + 1));
            }
            else {
                this.setRecepient("self");
            }
        }

        //Randomly generate recepient for private statuses
        if (this.getStatus().equals("private")) {
            if ((rngNum = rng.nextInt(100)) < 90) {
                while (this.setRecepient("u" + (rng.nextInt(10000) + 1)).equals(this.getUser())){
                    ;
                }
            }
            else {
                this.setRecepient("self");
            }
        }

        //Randomly generate recepient for protected statuses
        if (this.getStatus().equals("protected")) {
            if ((rngNum = rng.nextInt(100)) < 85) {
                this.setRecepient("subscribers");
            }
            else if (rngNum < 95) {
                while (this.setRecepient("u" + (rng.nextInt(10000) + 1)).equals(this.getUser())){
                    ;
                }
            }
            else {
                this.setRecepient("self");
            }
        }

        //Randomly decide whether or not a message is in-response
        if (rng.nextInt(100) < 70) {
            this.setInResponse(rng.nextInt(curMsg) - 1);
        }
        else {
            this.setInResponse(-1);
        }

        //Randomly generate string of text from input file
        numWords = rng.nextInt(19) + 2;

        while (numWords-- > 0) {
            line += lines.get(rng.nextInt(lines.size()));
            if (numWords != 0) {
                line += " ";
            }
        }

        this.setText(line);
    }
}