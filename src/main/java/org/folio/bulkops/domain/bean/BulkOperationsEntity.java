package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.TenantNotePair;

import java.util.List;

/**
 * Marker interface for entities for which bulk operations is applicable.
 */
public interface BulkOperationsEntity {
  String getIdentifier(IdentifierType identifierType);
  Integer _version();
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
