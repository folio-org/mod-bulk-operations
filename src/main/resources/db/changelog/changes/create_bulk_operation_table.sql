CREATE TYPE OperationType as ENUM ('UPDATE', 'DELETE');
CREATE TYPE EntityType as ENUM ('USER', 'ITEM', 'HOLDING');
CREATE TYPE EntityCustomIdentifierType as ENUM ('UUID', 'BARCODE', 'EXTERNAL_ID', 'IDENTITY_NAME');
CREATE TYPE Status as ENUM ('NEW', 'RETRIEVING_RECORDS', 'SAVING_RECORDS_LOCALLY', 'DATA_MODIFICATION', 'REVIEW_CHANGES', 'APPLY_CHANGES', 'SUSPENDED', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'CANCELLED', 'SCHEDULED', 'FAILED');

CREATE TABLE IF NOT EXISTS bulk_operation  (
	id UUID PRIMARY KEY,
	user_id UUID,
	operation_type OperationType,
	entity_type EntityType,
	entity_custom_identifier_type EntityCustomIdentifierType,
	status Status,
	data_export_job_id UUID,
	link_to_origin_file TEXT,
	link_to_modified_file TEXT,
	link_to_result_file TEXT,
	total_num_of_records INT,
	processed_num_of_records INT,
	execution_chunk_size INT,
	start_time TIMESTAMP default now(),
	end_time TIMESTAMP default now(),
	error_message TEXT
);
