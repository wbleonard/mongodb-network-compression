import params
import sys
import time
from datetime import datetime
from pymongo import MongoClient
import argparse

# Process arguments
parser = argparse.ArgumentParser(description='MongoDB Network Compression Test')
parser.add_argument('-c', '--compressor', help="The compressor to use.", 
    choices=["snappy", 'zlib', 'zstd'])
args = parser.parse_args()

print("\nMongoDB Network Compression Test")

# Establish connection to MongoDB
if args.compressor is None:
    client = MongoClient(params.target_conn_string)
    print ('Network Compression: Off')
else:
    # https://pymongo.readthedocs.io/en/stable/api/pymongo/mongo_client.html?highlight=compression#pymongo.mongo_client.MongoClient
    client = MongoClient(params.target_conn_string, compressors=args.compressor, zlibCompressionLevel=9)
    print('Network Compression: ' + args.compressor)
    
print("Now:", datetime.now(), "\n")

db = client[params.target_read_database]
collection = db[params.target_read_collection]

bytesOut_start = db.command({'serverStatus' :1})["network"]["physicalBytesOut"]

read_count = 0  # Count the records read
t_start = time.time()

print ("Collection to read from: {}.{}".format(params.target_read_database, params.target_read_collection))
print ("Bytes to read: {} MB".format(params.megabytes_to_read))
print ("Bulk read size: {} records\n".format(params.batch_size))

# Convert parameter to bytes
bytes_to_read = params.megabytes_to_read*1000*1000
bytes_read = 0

while bytes_read < bytes_to_read:

    #print ('bytes read:', str(bytes_read))

    try:

        batch_size_bytes = params.batch_size_mb * 1000 * 1000

        cursor = collection.find().limit(params.batch_size).batch_size(params.batch_size)
        for record in cursor:
            record_size = sys.getsizeof(record)    
            bytes_read += record_size
            read_count += 1
      
            # If the modulus falls in the range of the batch size, report the results:
            if(bytes_read % batch_size_bytes in range(0-record_size, 0+record_size)):

                # Print performance stats
                duration = time.time()-t_start
                insert_secs = str(round(duration, 2))
                records_per_second = round(read_count/duration)
                print('{:.0f} megabytes read'.format(bytes_read/1000/1000), 'at {:.1f} kilobytes/second'.format(bytes_read/duration/1000))
                #print('({:.0f} records read'.format(read_count), 'at {:.1f} records/second)'.format(records_per_second))

    except KeyboardInterrupt:
        print
        sys.exit(0)

    except Exception as e:
        print('\n********\nConnection Problem:')
        print(e)
        print('********')
        sys.exit(0)

duration = time.time()-t_start
print("\n", read_count, 'records read in {} seconds'.format(str(round(duration))), '({:.1f} records/second)'.format(round(read_count/duration, 2)))

bytesOut_end = db.command({'serverStatus' :1})["network"]["physicalBytesOut"]
mbytesOut = round((bytesOut_end - bytesOut_start) / (1000 * 1000), 3)
print ("\n MongoDB Server Reported Megabytes Out: {} MB\n".format(mbytesOut))

