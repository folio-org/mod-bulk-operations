TRUNCATE TABLE bulk_operation_data_processing;

ALTER TABLE bulk_operation_data_processing
DROP COLUMN IF EXISTS id;

ALTER TABLE bulk_operation_data_processing
ADD PRIMARY KEY (bulk_operation_id);
