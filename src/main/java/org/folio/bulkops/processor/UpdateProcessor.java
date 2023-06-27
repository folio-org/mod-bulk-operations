package org.folio.bulkops.processor;

import java.util.UUID;

public interface UpdateProcessor<T> {
  void updateRecord(T t, String identifier,  UUID operationId);
  Class<T> getUpdatedType();
}
