target_conn_string = 'mongodb://localhost'
target_database = 'test'
target_collection = 'network-compression-test'

# Set to an empty string to turn off compression
compressor = ''           # No compression
compressor = 'snappy'
 
# Tunables
drop_collection = True    # Drop collection on run
megabytes_to_insert = 10
batch_size_mb = 1         # Batch size of bulk insert in megabytes

