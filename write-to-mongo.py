import params
import sys
import time
from datetime import datetime
from pymongo import MongoClient
from faker import Faker

print("\nMongoDB Network Compression Test")

# Establish connection to MongoDB

if params.compressor == '':
    client = MongoClient(params.target_conn_string)
    print ('Network Compression: Off')
else:
    # https://pymongo.readthedocs.io/en/stable/api/pymongo/mongo_client.html?highlight=compression#pymongo.mongo_client.MongoClient
    client = MongoClient(params.target_conn_string, compressors='snappy')
    print('Network Compression: Snappy')
    
print("Now:", datetime.now(), "\n")

db = client[params.target_database]
collection = db[params.target_collection]

## TODO: Add measurement using db.serverStat().network.bytesIn
bytesIn_start = db.command({'serverStatus' :1})["network"]["physicalBytesIn"]

if params.drop_collection:
    collection.drop()

fake = Faker()
insert_count = 0  # Count the records inserted.
t_start = time.time()

customers = []  # customers array for bulk insert

print ("Bytes to insert: {} MB".format(params.megabytes_to_insert))
print ("Bulk insert batch size {} MB\n".format(params.batch_size_mb))

# Convert parameter to bytes
bytes_to_insert = params.megabytes_to_insert*1000*1000
bytes_inserted = 0

while bytes_inserted < bytes_to_insert:

    try:

        customer = {
            "name": fake.name(),
            "email": fake.ascii_email(),
            "phone_number": fake.phone_number(),
            "job": fake.job(),
            "address": {
                "street": fake.street_address(),
                "city": fake.city(),
                "postcode": fake.postcode(),
            },
            "ssn": fake.ssn(),
            "credit_card": {
                "provider": fake.credit_card_provider(),
                "number": fake.credit_card_number(),
                "expiration_date": fake.credit_card_expire(),
                "security_code": fake.credit_card_security_code()
            },
            "license_plate": fake.license_plate(),
            "company": {
                "name": fake.company(),
                "catch_phrase": fake.catch_phrase(),
                "bs": fake.bs()
            },
            "notes": fake.text()
        }

        record_size = sys.getsizeof(customer)
        bytes_inserted += record_size

        customers.append(customer)

        insert_count += 1
        batch_size_bytes = params.batch_size_mb * 1000 * 1000
       
        # If the modulus falls in the range of the record size, insert the batch.
        if(bytes_inserted % batch_size_bytes in range(0-record_size, 0+record_size)):
            collection.insert_many(customers)
            customers = []

            # Print performance stats
            duration = time.time()-t_start
            insert_secs = str(round(duration, 2))
            records_per_second = str(round(insert_count/duration))
            print('{:.0f} megabytes inserted'.format(bytes_inserted/1000/1000), 'at {:.1f} kilobytes/second'.format(bytes_inserted/duration/1000))

    except KeyboardInterrupt:
        print
        sys.exit(0)

    except:
        print('\n********\n\nConnection problem\n\n********\n')

print("\n", insert_count, 'records inserted in', str(round(time.time()-t_start, 0)), 'seconds')

bytesIn_end = db.command({'serverStatus' :1})["network"]["physicalBytesIn"]
mbytesIn = round((bytesIn_end - bytesIn_start) / (1000 * 1000), 3)
print ("\n MongoDB Server Reported Megabytes In: {} MB\n".format(mbytesIn))

