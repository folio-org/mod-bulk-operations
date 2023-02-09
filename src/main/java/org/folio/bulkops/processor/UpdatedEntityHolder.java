package org.folio.bulkops.processor;

import lombok.Builder;
import lombok.Data;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;

@Data
@Builder
public class UpdatedEntityHolder<T extends BulkOperationsEntity> {
  private T entity;
  boolean isChanged;
}
