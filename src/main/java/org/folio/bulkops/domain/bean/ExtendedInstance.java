package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.IdentifierType;

import java.util.UUID;

@Data
@With
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ExtendedInstance implements BulkOperationsEntity {

  @JsonProperty("tenantId")
  private String tenantId;
  @JsonProperty("entity")
  private Instance entity;

  @Override
  public String getIdentifier(IdentifierType identifierType) {
    return entity.getIdentifier(identifierType);
  }

  @Override
  public Integer _version() {
    return entity._version();
  }

  @Override
  public boolean isMarcInstance() {
    return entity.isMarcInstance();
  }

  @Override
  public String getId() {
    return entity.getId();
  }

  @Override
  public BulkOperationsEntity getRecordBulkOperationEntity() {
    return entity;
  }

  @Override
  public String getTenant() {
    return tenantId;
  }
}
