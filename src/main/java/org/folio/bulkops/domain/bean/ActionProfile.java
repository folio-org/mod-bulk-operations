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
public class ActionProfile {

  @JsonProperty("id")
  private String id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("description")
  private String description;

  @JsonProperty("action")
  private String action;

  @JsonProperty("folioRecord")
  private String folioRecord;

  @JsonProperty("hidden")
  private Boolean hidden;

  @JsonProperty("remove9Subfields")
  private Boolean remove9Subfields;

}
