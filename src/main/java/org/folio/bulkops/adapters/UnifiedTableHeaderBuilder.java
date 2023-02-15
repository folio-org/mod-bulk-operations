package org.folio.bulkops.adapters;

import org.folio.bulkops.domain.dto.UnifiedTable;

public interface UnifiedTableHeaderBuilder<T> {
  UnifiedTable getEmptyTableWithHeaders();
  Class<T> getProcessedType();
}
