package org.folio.bulkops.processor.preview;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.Row;

public interface PreviewRowProcessor<T extends BulkOperationsEntity> {
  Row transformToRow(T entity);
  Class<T> getProcessedType();
}
