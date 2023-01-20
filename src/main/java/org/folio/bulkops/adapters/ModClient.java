package org.folio.bulkops.adapters;

import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.bean.IdentifierType;
import org.folio.bulkops.domain.dto.UnifiedTable;

import java.util.UUID;

public interface ModClient<T> {

  UnifiedTable convertEntityToUnifiedTable(T entity, UUID bulkOperationId, IdentifierType identifierType);
  Row convertEntityToUnifiedTableRow(T entity);
  UnifiedTable getUnifiedRepresentationByQuery(String query, long offset, long limit);
  UnifiedTable getEmptyTableWithHeaders();
  Class<T> getProcessedType();
}
