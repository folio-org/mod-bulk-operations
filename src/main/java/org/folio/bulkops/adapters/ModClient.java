package org.folio.bulkops.adapters;

import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.domain.dto.IdentifierType;

import java.util.UUID;

public interface ModClient<T> {

  UnifiedTable convertEntityToUnifiedTable(T entity, UUID bulkOperationId, IdentifierType identifierType);

  UnifiedTable getUnifiedRepresentationByQuery(String query, long offset, long limit);
}
