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
public class MappingProfile {

  @JsonProperty("id")
  private String id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("description")
  private String description;

  @JsonProperty("incomingRecordType")
  private String incomingRecordType;

  @JsonProperty("existingRecordType")
  private String existingRecordType;

  @JsonProperty("mappingDetails")
  private MappingDetails mappingDetails;

  @JsonProperty("hidden")
  private Boolean hidden;
}
