target_conn_string = 'mongodb://localhost'

# Tunables

# Read from Mongo
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


