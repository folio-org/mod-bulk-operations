package org.folio.bulkops.processor;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.exception.RecordDeleteException;

public interface RecordDeleteProcessor<T extends BulkOperationsEntity> {

  /**
   * This method deletes entity of type {@link T}.
   *
   * @param entity original entity of type {@link T}
   */
  void delete(T entity) throws RecordDeleteException;
}
