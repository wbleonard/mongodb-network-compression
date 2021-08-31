# MongoDB Network Compression

An under advertised feature of MongoDB is it's ability to compress data between the client and the server. Close has a really [nice article](https://making.close.com/posts/mongodb-network-compression}) on how compression reduced their network traffic from about 140 Mbps to 65 Mpbs. As Close notes, with cloud data transfer costs ranging from $0.01 per GB and up, you can get a nice little savings with a simple configuration change. 

MongoDB supports the following compressors:

* [snappy](https://docs.mongodb.com/manual/reference/glossary/#std-term-snappy})
* [zlib](https://docs.mongodb.com/manual/reference/glossary/#std-term-zlib]) (Available starting in MongoDB 3.6)
* [zstd](https://docs.mongodb.com/manual/reference/glossary/#std-term-zlib) (Available starting in MongoDB 4.2)

This repository contains a tuneable Python script that you can use to see the impact of network compression yourself. The initial iteration uses [snappy](https://docs.mongodb.com/manual/reference/glossary/#std-term-snappy}).

## Setup

Snappy compression in Python requires the python-snappy package.

```pip3 install python-snappy```




