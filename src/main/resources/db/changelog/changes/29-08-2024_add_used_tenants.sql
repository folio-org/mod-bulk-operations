ALTER TABLE bulk_operation
ADD COLUMN IF NOT EXISTS tenant_note_pairs JSONB DEFAULT '[]'::jsonb;

ALTER TABLE bulk_operation
ADD COLUMN IF NOT EXISTS used_tenants TEXT[];
