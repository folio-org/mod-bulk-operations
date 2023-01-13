package org.folio.bulkops.domain.bean;

/**
 * Marker interface for entities for which bulk operations is applicable.
 */
public abstract class BulkOperationsEntity {
  public abstract String getIdentifier(IdentifierType identifierType);
}
