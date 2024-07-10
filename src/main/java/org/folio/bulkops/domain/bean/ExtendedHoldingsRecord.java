package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.IdentifierType;

import java.util.List;

@Data
@With
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ExtendedHoldingsRecord implements BulkOperationsEntity, ElectronicAccessEntity {

  @JsonProperty("tenantId")
  private String tenantId;
  @JsonProperty("entity")
  private HoldingsRecord entity;

  @Override
  public String getIdentifier(IdentifierType identifierType) {
    return entity.getIdentifier(identifierType);
  }

  @Override
  public Integer _version() {
    return entity._version();
  }

  @Override
  public BulkOperationsEntity getRecordBulkOperationEntity() {
    return entity;
  }

  @JsonIgnore
  public List<ElectronicAccess> getElectronicAccess() {
    return entity.getElectronicAccess();
  }
}
