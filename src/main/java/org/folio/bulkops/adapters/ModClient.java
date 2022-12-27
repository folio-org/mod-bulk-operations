package org.folio.bulkops.adapters;

import java.util.UUID;

import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.UnifiedTable;

public interface ModClient<T> {

  UnifiedTable convertEntityToUnifiedTable(T entity, UUID bulkOperationId, IdentifierType identifierType);
  UnifiedTable getUnifiedRepresentationByQuery(String query, long offset, long limit);
  Class<T> getProcessedType();
}
