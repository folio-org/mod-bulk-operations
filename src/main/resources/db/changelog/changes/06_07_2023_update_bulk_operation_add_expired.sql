ALTER TABLE bulk_operation
ADD COLUMN IF NOT EXISTS expired BOOLEAN DEFAULT FALSE;
