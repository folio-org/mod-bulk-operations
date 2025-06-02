CREATE TABLE IF NOT EXISTS profile (
  id UUID PRIMARY KEY,
  name VARCHAR(256),
  description TEXT,
  locked boolean,
  entity_type VARCHAR(128) CHECK (entity_type IN('USER', 'ITEM', 'HOLDINGS_RECORD', 'INSTANCE', 'INSTANCE_MARC')),
  bulk_operation_rule_collection jsonb,
  bulk_operation_marc_rule_collection jsonb,
  created_date TIMESTAMP,
  created_by UUID,
  created_by_user VARCHAR(64),
  updated_date TIMESTAMP,
  updated_by UUID,
  updated_by_user VARCHAR(64)
);
