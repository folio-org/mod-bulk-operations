CREATE TABLE IF NOT EXISTS bulk_operation_marc_rule (
  id UUID PRIMARY KEY,
  bulk_operation_id UUID,
  user_id UUID,
  tag TEXT,
  ind1 TEXT,
  ind2 TEXT,
  subfield TEXT,
  actions jsonb,
  parameters jsonb,
  subfields jsonb,
  constraint fk_marc_rule_to_operation foreign key (bulk_operation_id)
   references bulk_operation(id) ON DELETE CASCADE);

ALTER TYPE EntityType RENAME VALUE 'INSTANCE' TO 'INSTANCE_FOLIO';
ALTER TYPE EntityType ADD VALUE 'INSTANCE_MARC';

ALTER TYPE UpdateActionType ADD VALUE 'APPEND';
ALTER TYPE UpdateActionType ADD VALUE 'ADDITIONAL_SUBFIELD';
ALTER TYPE UpdateActionType ADD VALUE 'REMOVE_FIELD';
ALTER TYPE UpdateActionType ADD VALUE 'REMOVE_SUBFIELD';
