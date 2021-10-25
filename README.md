# MongoDB Network Compression - A Win-Win

An under-advertised feature of MongoDB is its ability to compress data between the client and the server. The CRM company Close has a really [nice article](https://making.close.com/posts/mongodb-network-compression) on how compression reduced their network traffic from about 140 Mbps to 65 Mpbs. As Close notes, with cloud data transfer costs ranging from $0.01 per GB and up, you can get a nice little savings with a simple configuration change. 

![mongodb-network-compression-chart](img/mongodb-network-compression-chart.webp)

MongoDB supports the following compressors:

* [snappy](https://docs.mongodb.com/manual/reference/glossary/#std-term-snappy)
* [zlib](https://docs.mongodb.com/manual/reference/glossary/#std-term-zlib) (Available starting in MongoDB 3.6)
* [zstd](https://docs.mongodb.com/manual/reference/glossary/#std-term-zlib) (Available starting in MongoDB 4.2)

Enabling compression from the [client](https://pymongo.readthedocs.io/en/stable/api/pymongo/mongo_client.html) simply involves installing the desired compression library and then passing the desired compressor as an argument when you connect to MongoDB. For example:

```PYTHON
client = MongoClient('mongodb://localhost', compressors='snappy')
```


This repository contains two tuneable Python scripts, [read-from-mongo.py](read-from-mongo.py) and [write-to-mongo.py](write-to-mongo.py), that you can use to see the impact of network compression yourself. 

## Setup

### Client Configuration

Edit [params.py](params.py) and at a minimum, set your connection string. Other tunables include the amount of bytes to read and insert (default 10 MB) and the batch to read (100 records) and insert (1 MB):

``` PYTHON
# Read to Mongo
target_read_database        = 'sample_airbnb'
target_read_collection      = 'listingsAndReviews'
megabytes_to_read           = 10
batch_size                  = 100   # Batch size in records (for reads)

# Write to Mongo
drop_collection             = True  # Drop collection on run
target_write_database       = 'test'
target_write_collection     = 'network-compression-test'
megabytes_to_insert         = 10
batch_size_mb               = 1     # Batch size of bulk insert in megabytes
```
### Compression Library
The [snappy](https://docs.mongodb.com/manual/reference/glossary/#std-term-snappy) compression in Python requires the `python-snappy` package.

```pip3 install python-snappy```

The [zstd](https://docs.mongodb.com/manual/reference/glossary/#std-term-zlib) compression requires the zstandard package

```pip3 install zstandard```

The [zlib](https://docs.mongodb.com/manual/reference/glossary/#std-term-zlib) compression is native to Python.

### Sample Data
My [read-from-mongo.py](read-from-mongo.py) script uses the [Sample AirBnB Listings Dataset](https://docs.atlas.mongodb.com/sample-data/sample-airbnb/#std-label-sample-airbnb) but ANY dataset will suffice for this test. 

The [write-to-mongo.py](write-to-mongo.py) script generates sample data using the Pyhon package 
[Faker](https://faker.readthedocs.io/en/master/).

```pip3 install faker ```

## Execution
### Read from Mongo
The cloud providers notabaly charge for data egress, so anything that reduces network traffic out is a win. 

Let's first run the script without network compression (the default):

```ZSH
python3 read-from-mongo.py

MongoDB Network Compression Test
Network Compression: Off
Now: 2021-10-25 11:04:00.115548 

Collection to read from: sample_airbnb.listingsAndReviews
Bytes to read: 10 MB
Bulk read size: 100 records

1 megabytes read at 324.4 kilobytes/second
2 megabytes read at 307.5 kilobytes/second
3 megabytes read at 327.8 kilobytes/second
4 megabytes read at 329.3 kilobytes/second
5 megabytes read at 333.1 kilobytes/second
6 megabytes read at 330.3 kilobytes/second
7 megabytes read at 332.4 kilobytes/second
8 megabytes read at 325.5 kilobytes/second
9 megabytes read at 328.0 kilobytes/second
10 megabytes read at 325.4 kilobytes/second

 8600 records read in 31 seconds (279.8 records/second)

 MongoDB Server Reported Megabytes Out: 191.234 MB
 ```

_You've obviously noticed the report Megabytes out (191 MB) are almsot 20 times our test size of 10 MBs. There are several reasons for this, including other workloads running on the server, data replication to secondary nodes, and the TCP packet being larger than just the data. Focus on the delta between the 2 tests runs._

The script accepts an optional compression argument, that must be either `snappy`, `zlib` or `zstd`. Let's run the test again using `snappy`:

```ZSH
✗ python3 read-from-mongo.py -c 'snappy'

MongoDB Network Compression Test
Network Compression: snappy
Now: 2021-10-25 11:10:57.247084 

Collection to read from: sample_airbnb.listingsAndReviews
Bytes to read: 10 MB
Bulk read size: 100 records

1 megabytes read at 462.7 kilobytes/second
2 megabytes read at 445.3 kilobytes/second
3 megabytes read at 465.0 kilobytes/second
4 megabytes read at 458.7 kilobytes/second
5 megabytes read at 469.5 kilobytes/second
6 megabytes read at 462.7 kilobytes/second
7 megabytes read at 469.7 kilobytes/second
8 megabytes read at 469.0 kilobytes/second
9 megabytes read at 474.8 kilobytes/second
10 megabytes read at 473.7 kilobytes/second

 8600 records read in 21 seconds (407.4 records/second)

 MongoDB Server Reported Megabytes Out: 105.535 MB
```
With `snappy` compression, our reported bytes out about 86 MBs fewer. That's a 45% savings. But wait, the 10 MBs of data was read in 10 fewer seconds. That's a 32% performance boost!

Let's try this again using `zlib`. 

_[zlib](https://docs.mongodb.com/manual/reference/glossary/#std-term-zlib) compression supports an optional compression level. For this test I've set it to `9` (max compression)._

```ZSH
✗ python3 read-from-mongo.py -c 'zlib'  

MongoDB Network Compression Test
Network Compression: zlib
Now: 2021-10-25 11:31:36.851154 

Collection to read from: sample_airbnb.listingsAndReviews
Bytes to read: 10 MB
Bulk read size: 100 records

1 megabytes read at 367.5 kilobytes/second
2 megabytes read at 376.5 kilobytes/second
3 megabytes read at 385.5 kilobytes/second
4 megabytes read at 389.4 kilobytes/second
5 megabytes read at 397.2 kilobytes/second
6 megabytes read at 391.2 kilobytes/second
7 megabytes read at 398.9 kilobytes/second
8 megabytes read at 400.2 kilobytes/second
9 megabytes read at 402.6 kilobytes/second
10 megabytes read at 401.5 kilobytes/second

 8600 records read in 25 seconds (345.2 records/second)

 MongoDB Server Reported Megabytes Out: 66.324 MB
 ```
 With `zlib` compression configured at its maximum compression level, we were able to achieve a `65%` reduction in network egress, although it took 4 seconds longer. However, that's still a `19%` pefomance improvement over using no compression at all.

### Write to Mongo

The cloud providers often don't charge us for data ingress. However, given the substantial performance improvements with read workloads, what can be expected from write workloads?

he [write-to-mongo.py](write-to-mongo.py) script writes a randomly generated document to the database and collection configured in [params.py](params.py), the default being `test.network_compression_test`.

As before let's run the test without compression:

```ZSH
✗ python3 write-to-mongo.py

MongoDB Network Compression Test
Network Compression: Off
Now: 2021-10-25 11:50:10.035349 

Bytes to insert: 10 MB
Bulk insert batch size: 1 MB

1 megabytes inserted at 613.2 kilobytes/second
2 megabytes inserted at 609.1 kilobytes/second
3 megabytes inserted at 611.8 kilobytes/second
4 megabytes inserted at 600.6 kilobytes/second
5 megabytes inserted at 614.0 kilobytes/second
6 megabytes inserted at 623.4 kilobytes/second
7 megabytes inserted at 630.1 kilobytes/second
8 megabytes inserted at 634.9 kilobytes/second
9 megabytes inserted at 639.7 kilobytes/second
10 megabytes inserted at 643.2 kilobytes/second

 27778 records inserted in 16.0 seconds

 MongoDB Server Reported Megabytes In: 22.041 MB
```

So it took `16` seconds to write `27,778` records. Let's run the same test with `snappy` compression:



```zsh
✗ python3 write-to-mongo.py -c 'snappy'

MongoDB Network Compression Test
Network Compression: snappy
Now: 2021-10-25 11:52:11.973121 

Bytes to insert: 10 MB
Bulk insert batch size: 1 MB

1 megabytes inserted at 633.3 kilobytes/second
2 megabytes inserted at 648.4 kilobytes/second
3 megabytes inserted at 655.7 kilobytes/second
4 megabytes inserted at 645.4 kilobytes/second
5 megabytes inserted at 634.9 kilobytes/second
6 megabytes inserted at 642.5 kilobytes/second
7 megabytes inserted at 646.3 kilobytes/second
8 megabytes inserted at 649.0 kilobytes/second
9 megabytes inserted at 652.2 kilobytes/second
10 megabytes inserted at 654.8 kilobytes/second

 27778 records inserted in 15.0 seconds

 MongoDB Server Reported Megabytes In: 11.237 MB
 ```
Our reported megabytes in reduced by `49%`. However, our write performance only decreased by `1 second (6%)`. Not as great as with the read performance, but still worth a look. 
## Measurement

There are a couple of options for measuring network traffic. 

The MongoDB serverStatus [network](https://docs.mongodb.com/manual/reference/command/serverStatus/#network) document reports on network use.

You can see from the tests above, inserting 10 MBs of data using the `snappy` compressor reported `11.323 MB In`. With no compression, the same 10 MBs of data consumed `19.963 MB`.

_If you're wondering why the reported numbers are double the data inserted, that's due to other workloads running on the server, and the TCP packet being larger than just the data. Focus on the delta between the 2 tests runs._

Another option would be using a network analysis tool like [Wireshark](https://www.wireshark.org/). But that's beyond the scope of this article for now.

Bottom line, compression reduced Network traffic by about 50%, which is in line with the improvement seen by Close. 


