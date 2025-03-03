ALTER TABLE bulk_operation ALTER COLUMN operation_type TYPE VARCHAR(128) USING operation_type::TEXT;
ALTER TABLE bulk_operation ALTER COLUMN identifier_type TYPE VARCHAR(128) USING identifier_type::TEXT;
ALTER TABLE bulk_operation ALTER COLUMN status TYPE VARCHAR(128) USING status::TEXT;
ALTER TABLE bulk_operation ALTER COLUMN approach TYPE VARCHAR(128) USING approach::TEXT;
ALTER TABLE bulk_operation ALTER COLUMN entity_type TYPE VARCHAR(128) USING entity_type::TEXT;
ALTER TABLE bulk_operation_data_processing ALTER COLUMN status TYPE VARCHAR(128) USING status::TEXT;
ALTER TABLE bulk_operation_execution ALTER COLUMN status TYPE VARCHAR(128) USING status::TEXT;
ALTER TABLE bulk_operation_execution_chunk ALTER COLUMN state TYPE VARCHAR(128) USING state::TEXT;
ALTER TABLE bulk_operation_execution_content ALTER COLUMN state TYPE VARCHAR(128) USING state::TEXT;
ALTER TABLE bulk_operation_execution_content ALTER COLUMN error_type TYPE VARCHAR(128) USING error_type::TEXT;
ALTER TABLE bulk_operation_processing_content ALTER COLUMN state TYPE VARCHAR(128) USING state::TEXT;
ALTER TABLE bulk_operation_rule ALTER COLUMN update_option TYPE VARCHAR(128) USING update_option::TEXT;
ALTER TABLE bulk_operation_rule_details ALTER COLUMN update_action TYPE VARCHAR(128) USING update_action::TEXT;


DROP TYPE IF EXISTS approachtype CASCADE;
DROP TYPE IF EXISTS entitytype CASCADE;
DROP TYPE IF EXISTS errortype CASCADE;
DROP TYPE IF EXISTS statetype CASCADE;
DROP TYPE IF EXISTS identifiertype CASCADE;
DROP TYPE IF EXISTS operationstatustype CASCADE;
DROP TYPE IF EXISTS operationtype CASCADE;
DROP TYPE IF EXISTS statustype CASCADE;
DROP TYPE IF EXISTS updateactiontype CASCADE;
DROP TYPE IF EXISTS updateoptiontype CASCADE;
