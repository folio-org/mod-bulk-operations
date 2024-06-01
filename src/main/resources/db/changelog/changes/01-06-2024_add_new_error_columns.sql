ALTER TABLE bulk_operation_execution_content
ADD COLUMN IF NOT EXISTS link_to_failed_entity TEXT;
ALTER TABLE bulk_operation_execution_content
ADD COLUMN IF NOT EXISTS ui_error_message TEXT;
