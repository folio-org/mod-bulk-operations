ALTER TABLE bulk_operation
ADD COLUMN IF NOT EXISTS link_to_triggering_query_file TEXT;
