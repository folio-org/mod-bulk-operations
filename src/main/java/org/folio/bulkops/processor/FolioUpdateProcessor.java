package org.folio.bulkops.processor;

import org.folio.bulkops.domain.entity.BulkOperation;

public interface FolioUpdateProcessor<T> {
  void updateRecord(T t);

  void updateAssociatedRecords(T t, BulkOperation bulkOperation, boolean notChanged);

  Class<T> getUpdatedType();
}
