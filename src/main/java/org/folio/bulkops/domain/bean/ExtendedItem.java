package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.TenantNotePair;

@Data
@With
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ExtendedItem implements BulkOperationsEntity {

  @JsonProperty("tenantId")
  private String tenantId;

  @JsonProperty("entity")
  private Item entity;

  @Override
  public String getIdentifier(IdentifierType identifierType) {
    return entity.getIdentifier(identifierType);
  }

  @Override
  public Integer entityVersion() {
    return entity.entityVersion();
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

  @Override
  public void setTenantToNotes(List<TenantNotePair> tenantNotePairs) {
    entity
        .getNotes()
        .forEach(
            note -> {
              var tenantNotePair =
                  tenantNotePairs.stream()
                      .filter(pair -> pair.getNoteTypeId().equals(note.getItemNoteTypeId()))
                      .findFirst();
              if (tenantNotePair.isPresent()) {
                note.setTenantId(tenantNotePair.get().getTenantId());
                note.setItemNoteTypeName(tenantNotePair.get().getNoteTypeName());
              } else {
                note.setTenantId(tenantId);
              }
            });
  }

  @Override
  public String getId() {
    return entity.getId();
  }
}
