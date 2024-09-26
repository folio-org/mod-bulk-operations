ALTER TABLE bulk_operation_rule_details
ADD COLUMN IF NOT EXISTS tenants TEXT[];
