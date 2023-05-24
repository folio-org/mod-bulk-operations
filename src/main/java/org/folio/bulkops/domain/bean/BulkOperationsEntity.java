package org.folio.bulkops.domain.bean;

import org.folio.bulkops.domain.dto.IdentifierType;

import java.util.HashMap;
import java.util.Map;

/**
 * Marker interface for entities for which bulk operations is applicable.
 */
public abstract class BulkOperationsEntity {

  private final Map<String, Object> defaults = new HashMap<>();

  public void setDefaultValue(String field, Object value) {
    defaults.put(field, value);
  }

  public Object getDefaultValue(String field) {
    return defaults.get(field);
  }

  public abstract String getIdentifier(IdentifierType identifierType);
}
