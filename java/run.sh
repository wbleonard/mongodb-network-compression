export mongodbURI="mongodb://localhost:27017"
export compressor="snappy"   # Compressor - One of unset, "zstd", "snappy" or "zlib"
export megabytesToRead=10
export recordBatchSize=100


java -cp target/java-bootstrap-1.0-SNAPSHOT-jar-with-dependencies.jar com.mongodb.java.bootstrap.App




