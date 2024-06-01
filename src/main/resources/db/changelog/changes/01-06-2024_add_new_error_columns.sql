ALTER TABLE bulk_operation_data_processing
ADD COLUMN IF NOT EXISTS link TEXT;
ALTER TABLE bulk_operation_data_processing
ADD COLUMN IF NOT EXISTS ui_error_message TEXT;
