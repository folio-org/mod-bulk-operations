CREATE TYPE ErrorType as ENUM ('ERROR', 'WARNING');
CREATE CAST (character varying as ErrorType) WITH INOUT AS IMPLICIT;
ALTER TABLE bulk_operation_execution_content
ADD COLUMN IF NOT EXISTS error_type ErrorType DEFAULT 'ERROR';
