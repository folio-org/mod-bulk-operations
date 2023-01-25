-- noinspection SqlNoDataSourceInspectionForFile

CREATE TYPE OperationType as ENUM ('UPDATE', 'DELETE');
CREATE CAST (character varying as OperationType) WITH INOUT AS IMPLICIT;
CREATE TYPE EntityType as ENUM ('USER', 'ITEM', 'HOLDING');
CREATE CAST (character varying as EntityType) WITH INOUT AS IMPLICIT;
CREATE TYPE IdentifierType as ENUM ('ID', 'BARCODE', 'HRID', 'FORMER_IDS', 'ACCESSION_NUMBER', 'HOLDINGS_RECORD_ID', 'USER_NAME', 'EXTERNAL_SYSTEM_ID', 'INSTANCE_HRID', 'ITEM_BARCODE');
CREATE CAST (character varying as IdentifierType) WITH INOUT AS IMPLICIT;
CREATE TYPE OperationStatusType as ENUM ('NEW', 'RETRIEVING_RECORDS', 'SAVING_RECORDS_LOCALLY', 'DATA_MODIFICATION', 'REVIEW_CHANGES', 'APPLY_CHANGES', 'SUSPENDED', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'CANCELLED', 'SCHEDULED', 'FAILED');
CREATE CAST (character varying as OperationStatusType) WITH INOUT AS IMPLICIT;
CREATE TYPE ApproachType as ENUM ('IN_APP', 'MANUAL');
CREATE CAST (character varying as ApproachType) WITH INOUT AS IMPLICIT;

CREATE TABLE IF NOT EXISTS bulk_operation (
  id UUID PRIMARY KEY,
  user_id UUID,
  operation_type OperationType,
  entity_type EntityType,
  identifier_type IdentifierType,
  status OperationStatusType,
  approach ApproachType,
  data_export_job_id UUID,
  link_to_triggering_file TEXT,
  link_to_origin_file TEXT,
  link_to_matching_records_file TEXT,
  link_to_matching_errors_file TEXT,
  link_to_modified_file TEXT,
  link_to_the_preview_file TEXT,
  link_to_result_file TEXT,
  link_to_updated_records_file TEXT,
  link_to_committing_errors_file TEXT,
  total_num_of_records INT,
  processed_num_of_records INT,
  execution_chunk_size INT,
  start_time TIMESTAMP default now(),
  end_time TIMESTAMP default now(),
  error_message TEXT
);

CREATE TYPE StatusType as ENUM ('ACTIVE', 'COMPLETED', 'FAILED');
CREATE CAST (character varying as StatusType) WITH INOUT AS IMPLICIT;

CREATE TABLE IF NOT EXISTS bulk_operation_execution (
  id UUID PRIMARY KEY,
  bulk_operation_id UUID,
  user_id UUID,
  start_time TIMESTAMP default now(),
  end_time TIMESTAMP default now(),
  processed_records INT,
  status StatusType,
  constraint fk_execution_to_operation foreign key (bulk_operation_id)
    references bulk_operation(id) ON DELETE CASCADE
);

CREATE TYPE StateType as ENUM ('PROCESSED', 'FAILED');
CREATE CAST (character varying as StateType) WITH INOUT AS IMPLICIT;

CREATE TABLE IF NOT EXISTS bulk_operation_execution_chunk (
  id UUID PRIMARY KEY,
  bulk_operation_execution_id UUID,
  bulk_operation_id UUID,
  first_record_index INT,
  last_record_index INT,
  start_time TIMESTAMP default now(),
  end_time TIMESTAMP default now(),
  state StateType,
  error_message TEXT,
  constraint fk_execution_chunk_to_execution foreign key (bulk_operation_execution_id)
    references bulk_operation_execution(id),
  constraint fk_execution_chunk_to_operation foreign key (bulk_operation_id)
    references bulk_operation(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bulk_operation_execution_content (
  id UUID PRIMARY KEY,
  identifier TEXT,
  bulk_operation_execution_chunk_id UUID,
  bulk_operation_id UUID,
  state StateType,
  error_message TEXT,
  constraint fk_content_to_execution_chunk foreign key (bulk_operation_execution_chunk_id)
    references bulk_operation_execution_chunk(id),
  constraint fk_content_to_operation foreign key (bulk_operation_id)
    references bulk_operation(id) ON DELETE CASCADE
);

CREATE TYPE UpdateOptionType AS ENUM ('PATRON_GROUP', 'EXPIRATION_DATE', 'EMAIL_ADDRESS', 'PERMANENT_LOCATION', 'TEMPORARY_LOCATION', 'PERMANENT_LOAN_TYPE', 'TEMPORARY_LOAN_TYPE', 'STATUS');
CREATE CAST (character varying as UpdateOptionType) WITH INOUT AS IMPLICIT;

CREATE TABLE IF NOT EXISTS bulk_operation_rule (
  id UUID PRIMARY KEY,
  bulk_operation_id UUID,
  user_id UUID,
  update_option UpdateOptionType,
  constraint fk_rule_to_operation foreign key (bulk_operation_id)
    references bulk_operation(id) ON DELETE CASCADE
);

CREATE TYPE UpdateActionType AS ENUM ('ADD_TO_EXISTING', 'CLEAR_FIELD', 'FIND', 'FIND_AND_REMOVE_THESE', 'REPLACE_WITH', 'FIND_AND_REPLACE');
CREATE CAST (character varying as UpdateActionType) WITH INOUT AS IMPLICIT;

CREATE TABLE IF NOT EXISTS bulk_operation_rule_details (
  id UUID PRIMARY KEY,
  rule_id UUID,
  update_action UpdateActionType,
  initial_value TEXT,
  updated_value TEXT,
  constraint fk_rule_details_to_rule foreign key (rule_id)
    references bulk_operation_rule(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bulk_operation_data_processing (
  id UUID PRIMARY KEY,
  bulk_operation_id UUID,
  status StatusType,
  start_time TIMESTAMP default now(),
  end_time TIMESTAMP default now(),
  total_num_of_records INT,
  processed_num_of_records INT,
  constraint fk_data_processing_to_operation foreign key (bulk_operation_id)
   references bulk_operation(id) ON DELETE CASCADE
);
