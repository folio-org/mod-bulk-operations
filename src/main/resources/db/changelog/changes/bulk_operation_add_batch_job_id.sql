ALTER TABLE bulk_operation
ADD COLUMN IF NOT EXISTS batch_job_id BIGINT;
