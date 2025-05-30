DROP TYPE IF EXISTS EntityType CASCADE;
CREATE TYPE EntityType AS ENUM ('USER', 'ITEM', 'HOLDINGS_RECORD', 'INSTANCE', 'INSTANCE_MARC');
CREATE CAST (character varying as EntityType) WITH INOUT AS IMPLICIT;

CREATE TABLE IF NOT EXISTS profile (
  id UUID PRIMARY KEY,
  name VARCHAR(256),
  description TEXT,
  locked boolean,
  entity_type EntityType,
  bulk_operation_rule_collection jsonb,
  bulk_operation_marc_rule_collection jsonb,
  created_date TIMESTAMP,
  created_by UUID,
  created_by_username VARCHAR(64),
  updated_date TIMESTAMP,
  updated_by UUID,
  updated_by_username VARCHAR(64)
);

ALTER TABLE profile ALTER COLUMN entity_type TYPE VARCHAR(128) USING entity_type::TEXT;
ALTER TABLE profile ADD CONSTRAINT entity_type_check CHECK (entity_type IN ('USER', 'ITEM', 'HOLDINGS_RECORD', 'INSTANCE', 'INSTANCE_MARC'));
DROP TYPE IF EXISTS EntityType CASCADE;
