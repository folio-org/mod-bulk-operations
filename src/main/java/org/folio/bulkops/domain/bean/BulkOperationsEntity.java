package org.folio.bulkops.domain.bean;

import org.folio.bulkops.domain.dto.IdentifierType;

/**
 * Marker interface for entities for which bulk operations is applicable.
 */
public abstract class BulkOperationsEntity {
  public abstract String getIdentifier(IdentifierType identifierType);
}
