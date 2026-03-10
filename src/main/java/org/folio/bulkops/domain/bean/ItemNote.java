package org.folio.bulkops.domain.bean;

import static org.folio.bulkops.util.Constants.ITEM_NOTE_TYPE_NAME_KEY;
import static org.folio.bulkops.util.Constants.TENANT_ID_KEY;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ItemNote {
  @JsonProperty("itemNoteTypeId")
  private String itemNoteTypeId;

  @JsonProperty("note")
  private String note;

  @JsonProperty("staffOnly")
  private Boolean staffOnly = false;

  @JsonProperty(TENANT_ID_KEY)
  private String tenantId;

  @JsonProperty(ITEM_NOTE_TYPE_NAME_KEY)
  private String itemNoteTypeName;
}
