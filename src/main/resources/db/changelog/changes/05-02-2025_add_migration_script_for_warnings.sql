ALTER TABLE bulk_operation
ALTER COLUMN matched_num_of_warnings SET DEFAULT 0;

UPDATE bulk_operation
SET matched_num_of_warnings = 0
WHERE matched_num_of_warnings is null;
