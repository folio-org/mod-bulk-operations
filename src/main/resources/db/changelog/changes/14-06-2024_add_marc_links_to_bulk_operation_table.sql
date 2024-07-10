ALTER TABLE bulk_operation
ADD COLUMN IF NOT EXISTS link_to_matched_records_marc_file TEXT,
ADD COLUMN IF NOT EXISTS link_to_modified_records_marc_file TEXT,
ADD COLUMN IF NOT EXISTS link_to_committed_records_marc_file TEXT;
