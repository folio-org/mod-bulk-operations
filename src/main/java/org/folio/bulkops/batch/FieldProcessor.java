package org.folio.bulkops.batch;

public interface FieldProcessor {
  Object process(Object field, int i);
}
