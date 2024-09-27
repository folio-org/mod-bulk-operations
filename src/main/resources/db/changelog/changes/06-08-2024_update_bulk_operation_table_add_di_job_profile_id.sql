ALTER TABLE bulk_operation
ADD COLUMN IF NOT EXISTS data_import_job_profile_id uuid;
