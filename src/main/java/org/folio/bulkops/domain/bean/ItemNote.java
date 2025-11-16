package org.folio.bulkops.domain.bean;

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
  @JsonProperty("tenantId")
  private String tenantId;
  @JsonProperty("itemNoteTypeName")
  private String itemNoteTypeName;
}

