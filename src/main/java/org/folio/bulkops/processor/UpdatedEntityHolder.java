package org.folio.bulkops.processor;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdatedEntityHolder<T extends BulkOperationsEntity> {
  private T updated;
  boolean shouldBeUpdated;
  private int errors;
}
