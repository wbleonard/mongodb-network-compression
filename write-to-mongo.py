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
    # https://making.close.com/posts/mongodb-network-compression
    client = MongoClient(params.target_conn_string, compressors='snappy')
    print('Network Compression: Snappy')
    
print("Now:", datetime.now(), "\n")

db = client[params.target_database]
collection = db[params.target_collection]

if params.drop_collection:
    collection.drop()

fake = Faker()
insert_count = 0  # Count the records inserted.
t_start = time.time()

# Run for 1 minute
t_end = t_start + params.run_duration

# customers array for bulk insert
customers = []

while time.time() < t_end:

    ## TODO: Switch to bulk write

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

        customers.append(customer)

        #collection.insert_one(customer)
        insert_count += 1

        if(insert_count % params.batch_size == 0):
            collection.insert_many(customers)
            customers = []

            # Print performance stats
            duration = time.time()-t_start
            insert_secs = str(round(duration, 2))
            records_per_second = str(round(insert_count/duration))
            print(insert_count, 'records inserted in', insert_secs, 'seconds = ', records_per_second, 'records/second')

    except KeyboardInterrupt:
        print
        sys.exit(0)

    except:
        print('\n********\n\nConnection problem\n\n********\n')

print("\n", insert_count, 'records inserted in', str(
    round(time.time()-t_start, 0)), 'seconds')


