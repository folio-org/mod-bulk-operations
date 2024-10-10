ALTER TABLE bulk_operation_rule_details
RENAME COLUMN tenants TO rule_tenants;

ALTER TABLE bulk_operation_rule_details
ADD COLUMN IF NOT EXISTS action_tenants TEXT[];

