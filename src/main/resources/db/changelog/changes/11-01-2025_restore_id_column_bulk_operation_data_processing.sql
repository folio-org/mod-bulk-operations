TRUNCATE TABLE bulk_operation_data_processing;

ALTER TABLE bulk_operation_data_processing
ADD COLUMN IF NOT EXISTS id uuid;

ALTER TABLE bulk_operation_data_processing
DROP CONSTRAINT bulk_operation_data_processing_pkey;

ALTER TABLE bulk_operation_data_processing
ADD CONSTRAINT bulk_operation_data_processing_pkey PRIMARY KEY (id);
