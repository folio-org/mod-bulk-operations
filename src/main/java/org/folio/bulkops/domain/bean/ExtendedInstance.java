package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.IdentifierType;

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
}
