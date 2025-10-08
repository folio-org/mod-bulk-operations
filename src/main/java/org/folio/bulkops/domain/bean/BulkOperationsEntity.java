package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.TenantNotePair;

/**
 * Marker interface for entities for which bulk operations is applicable.
 */
public interface BulkOperationsEntity {
  String getIdentifier(IdentifierType identifierType);

  Integer entityVersion();

  @JsonIgnore
  default String getId() {
    return null;
  }

  @JsonIgnore
  default boolean isMarcInstance() {
    return false;
  }

  @JsonIgnore
  default String getSource() {
    return StringUtils.EMPTY;
  }

  @JsonIgnore
  default BulkOperationsEntity getRecordBulkOperationEntity() {
    return null;
  }

  @JsonIgnore
  default String getTenant() {
    return StringUtils.EMPTY;
  }

  @JsonIgnore
  default void setTenantToNotes(List<TenantNotePair> tenantNotePairs) {
  }

  @JsonIgnore
  default void setTenant(String tenantId) {
  }
}
