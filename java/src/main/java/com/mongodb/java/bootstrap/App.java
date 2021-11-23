package com.mongodb.java.bootstrap;

import com.mongodb.BasicDBObject;
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
        String databaseString = System.getenv("targetReadDatabase");
        String collectionString = System.getenv("targetReadCollection");
        String compressor = System.getenv("compressor");
        int megabytesToRead = Integer.parseInt(System.getenv("megabytesToRead"));
        int recordBatchSize = Integer.parseInt(System.getenv("recordBatchSize"));
        int reportBatchSizeMB = Integer.parseInt(System.getenv("reportBatchSizeMB"));
        long reportBatchSizeBytes = reportBatchSizeMB * 1000 * 1000;

        if (uriString == null) {
            System.out.format("%nMongoDB Connection String environment variable, 'mongodbURI', is not set");
            return;
        }

        if (compressor == null || compressor.isEmpty()) {
            System.out.format("%nNetwork Compression: Off");
            uriString = uriString + "&appName=Java Network Compress Test with no compressor";
        } else {
            System.out.format("%nNetwork Compression: " + compressor);
            uriString = uriString + "&compressors=" + compressor
                    + "&appName=Java Network Compress Test using Compressor " + compressor;
        }

        MongoDatabase database = null;

        com.mongodb.client.MongoClient mongoClient = MongoClients.create(uriString);
        database = mongoClient.getDatabase(databaseString);
        MongoCollection<Document> collection = database.getCollection(collectionString);

        long startTimeMillis = System.currentTimeMillis();
        Date currentDate = new Date(startTimeMillis);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.format("%nNow: %s", df.format(currentDate));

        // Get physical network bytes out at the start of th test
        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put("serverStatus", "1");
        Document serverStatus = database.runCommand(dbObject);
        Document network = (Document) serverStatus.get("network");
        long physicalBytesOutStart = network.getLong("physicalBytesOut");

        // https://mongodb.github.io/mongo-java-driver/4.4/apidocs/mongodb-driver-core/com/mongodb/MongoCompressor.html

        System.out.format("%n%nCollection to read from: %s.%s", databaseString, collectionString);
        System.out.format("%nBytes to read: %s MB", megabytesToRead);
        System.out.format("%nBatch read size: %s records\n", recordBatchSize);

        // Convert parameter to bytes
        long bytesToRead = (long) megabytesToRead * 1000 * 1000;
        long bytesRead = 0;
        int readCount = 0; // Count the records read

        while (bytesRead < bytesToRead) {

            try {

                // Limit the read to the batch size for reporting.
                MongoCursor<Document> cursor = collection.find().iterator();

                try {
                    while (cursor.hasNext()) {

                        Document document = cursor.next();
                        int recordSize = document.toJson().getBytes().length;
                        bytesRead += recordSize;
                        readCount++;

                        // But only print per megabyte read
                        long currentTimeMillis = System.currentTimeMillis();
                        long totalTimeMillis = currentTimeMillis - startTimeMillis;
                        double megabytesRead = bytesRead / 1000 / 1000;

                        // Since we're reporting on bytes, we report when the bytes read falls w/ our report 
                        // batch size, plus or minus the record size.
                        long modulus = bytesRead % reportBatchSizeBytes;
                        if ((modulus >= (0-recordSize)) && (modulus <= (0+recordSize))) {
                            //double kilobytesRead = ((double) bytesRead / 1000);
                            //double kilobytesPerSecond = ((double) (kilobytesRead / ((double) totalTimeMillis / 1000)));
                            double megabytesPerSecond = ((double) (megabytesRead / ((double) totalTimeMillis / 1000)));
                            System.out.format("%n%.0f megabytes read at %.3f megabytes/second", megabytesRead,
                                    megabytesPerSecond);
                        }

                        if (megabytesRead == megabytesToRead) {
                            System.out.format("%n%d megabytes read = %d megabytes to read", megabytesRead, megabytesToRead);
                            break;
                        }
                    }
                } finally {

                    cursor.close();
                }

            } catch (Exception e) {
                System.out.format("%n*** Exception: %s", e);
            }

        }

        long currentTimeMillis = System.currentTimeMillis();
        long totalTimeSecs = (currentTimeMillis - startTimeMillis) / 1000;
        System.out.format("%n%n%s records read in %d seconds (%d records/second)", readCount, totalTimeSecs,
                readCount / totalTimeSecs);

        serverStatus = database.runCommand(dbObject);
        network = (Document) serverStatus.get("network");
        long physicalBytesOutEnd = network.getLong("physicalBytesOut");
        double mbytesOut = ((double) (physicalBytesOutEnd - physicalBytesOutStart)) / (1000 * 1000);

        System.out.format("%nMongoDB ServerReported Megabytes Out: %.3f MB%n", mbytesOut);
    }
}
