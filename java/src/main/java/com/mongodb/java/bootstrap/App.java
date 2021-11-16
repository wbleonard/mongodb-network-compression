package com.mongodb.java.bootstrap;

import com.mongodb.client.MongoClients;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.mongodb.client.MongoDatabase;
import com.mongodb.internal.build.MongoDriverVersion;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class App {

    public static void main(String[] args) {

        // Control MongoDB Driver Logging...
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
        rootLogger.setLevel(Level.OFF);

        System.out.format("%nMongoDB Network Compression Test");

        String mongoDriverVersion = MongoDriverVersion.VERSION;
        System.out.format("%nMongoDB Driver Version: %s", mongoDriverVersion);

        // Grab the connection string from the environment:
        String uriString = System.getenv("mongodbURI");
        String compressor = System.getenv("compressor");
        int megaBytesToRead = Integer.parseInt(System.getenv("megabytesToRead"));
        int recordBatchSize = Integer.parseInt(System.getenv("recordBatchSize"));

        if (uriString == null) {
            System.out.format("%nMongoDB Connection String environment variable, 'mongodb_uri', is not set");
            return;
        }

        if (compressor == null) {
            System.out.format("%nNetwork Compression: Off");
            uriString = uriString + "&appName=Java Network Compress Test with no compressor";
        } else {
            System.out.format("%nNetwork Compression: " + compressor);
            uriString = uriString + "&compressors=" + compressor + "&appName=Java Network Compress Test using Compressor " + compressor;
        }

        String databaseString = "sample_analytics";
        String collectionString = "customers";
        MongoDatabase database = null;

        com.mongodb.client.MongoClient mongoClient = MongoClients.create(uriString);
        database = mongoClient.getDatabase(databaseString);
        MongoCollection<Document> collection = database.getCollection(collectionString);

        long startTimeMillis = System.currentTimeMillis();
        Date currentDate = new Date(startTimeMillis);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.format("%nNow: %s", df.format(currentDate));

        // The Java Driver doesn't appear to report ["network"]["physicalBytesOut"]
        // BasicDBObject serverStatus = new BasicDBObject();
        // serverStatus.put("ServerStatus", "1");
        // Document bytesOut_start = database.runCommand(serverStatus);
        // System.out.println("\n" + bytesOut_start.get("network") + "\n");

        // https://mongodb.github.io/mongo-java-driver/4.4/apidocs/mongodb-driver-core/com/mongodb/MongoCompressor.html

        System.out.format("%n%nCollection to read from: %s.%s", databaseString, collectionString);
        System.out.format("%nBytes to read: %s MB", megaBytesToRead);
        System.out.format("%nBatch read size: %s records\n", recordBatchSize);

        // Convert parameter to bytes
        int bytesToRead = megaBytesToRead * 1000 * 1000;
        int bytesRead = 0;
        long previousMegabytesRead = 0;
        int readCount = 0; // Count the records read

        while (bytesRead < bytesToRead) {

            // readCount = 0; // Rest the read count for each batch

            try {

                // Limit the read to the batch size for reporting.
                MongoCursor<Document> cursor = collection.find().limit(recordBatchSize).batchSize(recordBatchSize)
                        .iterator();

                try {
                    while (cursor.hasNext()) {

                        Document document = cursor.next();
                        int recordSize = document.toJson().getBytes().length;
                        bytesRead += recordSize;
                        readCount++;

                        // If the modulus falls in the range of the batch size, report the results:
                        // if (readCount % recordBatchSize == 0) {
                        // print('{:.0f} megabytes read'.format(bytes_read/1000/1000), 'at {:.1f}
                        // kilobytes/second'.format(bytes_read/duration/1000)
                    }
                } finally {

                    // Print performance stats
                    long currentTimeMillis = System.currentTimeMillis();
                    long totalTimeSecs = (currentTimeMillis - startTimeMillis)/1000;
                    long megabytesRead = bytesRead / 1000 / 1000;

                    if (megabytesRead > previousMegabytesRead) {
                        float rate = ((float) bytesRead/totalTimeSecs/1000/1000);
                        System.out.format("%n%d megabytes read at %.3f kilobytes/second", megabytesRead, rate );
                        previousMegabytesRead = megabytesRead;
                    }
                    cursor.close();
                }

            } catch (Exception e) {
                System.out.format("%n*** Exception: %s", e);
            }

        }

        long currentTimeMillis = System.currentTimeMillis();
        long totalTimeSecs = (currentTimeMillis - startTimeMillis)/1000;
        System.out.format("%n%n%s records read in %d seconds (%d records/second)", readCount, totalTimeSecs, readCount/totalTimeSecs);
    }
}
