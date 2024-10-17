package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.TenantNotePair;

import java.util.List;
import java.util.Optional;

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
    entity.setTenant(tenantId);
    return entity;
  }

  @Override
  public String getTenant() {
    return tenantId;
  }

  @JsonIgnore
  public List<ElectronicAccess> getElectronicAccess() {
    return entity.getElectronicAccess();
  }

  @Override
  public void setTenantToNotes(List<TenantNotePair> tenantNotePairs) {
    entity.getNotes().forEach(note -> {
      var tenantNotePair = tenantNotePairs.stream()
        .filter(pair -> pair.getNoteTypeId().equals(note.getHoldingsNoteTypeId()))
        .findFirst();
      if (tenantNotePair.isPresent()) {
        note.setTenantId(tenantNotePair.get().getTenantId());
        note.setHoldingsNoteTypeName(tenantNotePair.get().getNoteTypeName());
      } else {
        note.setTenantId(tenantId);
      }
    });
  }
}
