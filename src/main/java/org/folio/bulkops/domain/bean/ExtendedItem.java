package org.folio.bulkops.domain.bean;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.TenantNotePair;
import org.folio.bulkops.service.ItemReferenceHelper;

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

  @Override
  public void setTenantToNotes(List<TenantNotePair> tenantNotePairs) {
    entity.getNotes().forEach(note -> {
      var tenantNotePair = tenantNotePairs.stream()
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

  @Override
  public boolean isItem() {
    return true;
  }

  public void enrichItemWithReferenceDataNames() {
    if (nonNull(entity.getMaterialType()) && isNull(entity.getMaterialType().getName())) {
      var name = ItemReferenceHelper.service().getMaterialTypeById(entity.getMaterialTypeId(), tenantId).getName();
      entity.getMaterialType().setName(name);
    }
    if (nonNull(entity.getPermanentLoanType()) && isNull(entity.getPermanentLoanType().getName())) {
      var name = ItemReferenceHelper.service().getLoanTypeById(entity.getPermanentLoanTypeId(), tenantId).getName();
      entity.getPermanentLoanType().setName(name);
    }
    if (nonNull(entity.getTemporaryLoanType()) && isNull(entity.getTemporaryLoanType().getName())) {
      var name = ItemReferenceHelper.service().getLoanTypeById(entity.getTemporaryLoanTypeId(), tenantId).getName();
      entity.getTemporaryLoanType().setName(name);
    }
    if (nonNull(entity.getPermanentLocation()) && isNull(entity.getPermanentLocation().getName())) {
      var name = ItemReferenceHelper.service().getLocationById(entity.getPermanentLocationId(), tenantId).getName();
      entity.getPermanentLocation().setName(name);
    }
    if (nonNull(entity.getTemporaryLocation()) && isNull(entity.getTemporaryLocation().getName())) {
      var name = ItemReferenceHelper.service().getLocationById(entity.getTemporaryLocationId(), tenantId).getName();
      entity.getTemporaryLocation().setName(name);
    }
    if (nonNull(entity.getEffectiveLocation()) && isNull(entity.getEffectiveLocation().getName())) {
      var name = ItemReferenceHelper.service().getLocationById(entity.getEffectiveLocationId(), tenantId).getName();
      entity.getEffectiveLocation().setName(name);
    }
  }
}
