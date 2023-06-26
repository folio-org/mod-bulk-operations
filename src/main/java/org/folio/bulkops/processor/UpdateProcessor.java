package org.folio.bulkops.processor;

import java.util.UUID;

public interface UpdateProcessor<T> {
  void updateRecord(T t, UUID operationId);
  Class<T> getUpdatedType();
}
