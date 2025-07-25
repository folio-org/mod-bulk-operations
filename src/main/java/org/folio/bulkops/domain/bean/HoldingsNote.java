package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class HoldingsNote {
  @JsonProperty("holdingsNoteTypeId")
  private String holdingsNoteTypeId;

  @JsonProperty("holdingsNoteType")
  private HoldingsNoteType holdingsNoteType;

  @JsonProperty("note")
  private String note;

  @JsonProperty("staffOnly")
  private Boolean staffOnly;
  @JsonIgnore
  private String tenantId;
  private String holdingsNoteTypeName;
}

