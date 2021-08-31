
# Target 
target_conn_string = 'mongodb://localhost'
target_database = 'test'
target_collection = 'network-compression-test'

# Set to an empty string to turn off compression
# compressor = ''         # No compression
compressor = 'snappy'
 
# Tunables
drop_collection = True   # Drop collection on run
run_duration = 60
batch_size = 10000       # Batch size of bulk insert

