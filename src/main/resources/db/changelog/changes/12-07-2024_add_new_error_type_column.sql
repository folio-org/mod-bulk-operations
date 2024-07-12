CREATE TYPE BulkOperationStep as ENUM ('UPLOAD', 'EDIT', 'COMMIT');
CREATE CAST (character varying as BulkOperationStep) WITH INOUT AS IMPLICIT;

ALTER TABLE bulk_operation_execution_content
ADD COLUMN IF NOT EXISTS step BulkOperationStep;
