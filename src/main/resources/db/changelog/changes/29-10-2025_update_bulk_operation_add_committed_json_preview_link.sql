ALTER TABLE bulk_operation
ADD COLUMN IF NOT EXISTS link_to_committed_records_json_preview_file TEXT;
