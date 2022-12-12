package org.folio.bulkops.processor;

import org.folio.bulkops.domain.entity.BulkOperation;

public interface UpdateProcessor<T> {
  void updateRecord(T t);
}
