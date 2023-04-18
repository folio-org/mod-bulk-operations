ALTER TABLE bulk_operation
ALTER COLUMN total_num_of_records SET DEFAULT 0;
ALTER TABLE bulk_operation
ALTER COLUMN processed_num_of_records SET DEFAULT 0;
ALTER TABLE bulk_operation
ALTER COLUMN execution_chunk_size SET DEFAULT 0;
ALTER TABLE bulk_operation
ALTER COLUMN matched_num_of_records SET DEFAULT 0;
ALTER TABLE bulk_operation
ALTER COLUMN committed_num_of_records SET DEFAULT 0;
ALTER TABLE bulk_operation
ALTER COLUMN matched_num_of_errors SET DEFAULT 0;
ALTER TABLE bulk_operation
ALTER COLUMN committed_num_of_errors SET DEFAULT 0;

UPDATE bulk_operation
SET total_num_of_records = 0
WHERE total_num_of_records is null;

UPDATE bulk_operation
SET processed_num_of_records = 0
WHERE processed_num_of_records is null;

UPDATE bulk_operation
SET execution_chunk_size = 0
WHERE execution_chunk_size is null;

UPDATE bulk_operation
SET matched_num_of_records = 0
WHERE matched_num_of_records is null;

UPDATE bulk_operation
SET committed_num_of_records = 0
WHERE committed_num_of_records is null;

UPDATE bulk_operation
SET matched_num_of_errors = 0
WHERE matched_num_of_errors is null;

UPDATE bulk_operation
SET committed_num_of_errors = 0
WHERE committed_num_of_errors is null;