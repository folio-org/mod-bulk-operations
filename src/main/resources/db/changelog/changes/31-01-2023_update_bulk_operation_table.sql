CREATE TYPE ApproachType as ENUM ('IN_APP', 'MANUAL', 'QUERY');
CREATE CAST (character varying as ApproachType) WITH INOUT AS IMPLICIT;

DROP TYPE IF EXISTS EntityType CASCADE;
CREATE TYPE EntityType as ENUM ('USER', 'ITEM', 'HOLDINGS_RECORD');
CREATE CAST (character varying as EntityType) WITH INOUT AS IMPLICIT;

ALTER TABLE bulk_operation
ADD COLUMN IF NOT EXISTS approach ApproachType,
ADD COLUMN IF NOT EXISTS entity_type EntityType,
ADD COLUMN IF NOT EXISTS link_to_triggering_csv_file TEXT,
ADD COLUMN IF NOT EXISTS link_to_matched_records_csv_file TEXT,
ADD COLUMN IF NOT EXISTS link_to_matched_records_errors_csv_file TEXT,
ADD COLUMN IF NOT EXISTS link_to_modified_records_csv_file TEXT,
ADD COLUMN IF NOT EXISTS link_to_committed_records_csv_file TEXT,
ADD COLUMN IF NOT EXISTS link_to_committed_records_errors_csv_file TEXT,
ADD COLUMN IF NOT EXISTS matched_num_of_records INT,
ADD COLUMN IF NOT EXISTS committed_num_of_records INT,
ADD COLUMN IF NOT EXISTS matched_num_of_errors INT,
ADD COLUMN IF NOT EXISTS committed_num_of_errors INT,
DROP COLUMN IF EXISTS link_to_origin_file,
DROP COLUMN IF EXISTS link_to_modified_file,
DROP COLUMN IF EXISTS link_to_result_file,
ADD COLUMN IF NOT EXISTS link_to_matched_records_json_file TEXT,
ADD COLUMN IF NOT EXISTS link_to_modified_records_json_file TEXT,
ADD COLUMN IF NOT EXISTS link_to_modified_for_preview_records_json_file TEXT,
ADD COLUMN IF NOT EXISTS link_to_committed_records_json_file TEXT;
