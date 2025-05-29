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
