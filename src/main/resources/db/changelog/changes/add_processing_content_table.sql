CREATE TABLE IF NOT EXISTS bulk_operation_processing_content (
  id UUID PRIMARY KEY,
  identifier TEXT,
  bulk_operation_id UUID,
  state StateType,
  error_message TEXT,
  constraint fk_processing_content_to_operation foreign key (bulk_operation_id)
    references bulk_operation(id) ON DELETE CASCADE
);