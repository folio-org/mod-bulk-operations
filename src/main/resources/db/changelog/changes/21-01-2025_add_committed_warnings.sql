ALTER TABLE bulk_operation
ADD COLUMN IF NOT EXISTS committed_num_of_warnings INT;
ALTER TABLE bulk_operation
ALTER COLUMN committed_num_of_warnings SET DEFAULT 0;
UPDATE bulk_operation
SET committed_num_of_warnings = 0
WHERE committed_num_of_warnings is null;
