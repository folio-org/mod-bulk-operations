package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.List;

@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MappingDetails {

  @JsonProperty("name")
  private String name;

  @JsonProperty("recordType")
  private String recordType;

  @JsonProperty("marcMappingOption")
  private String marcMappingOption;

  @JsonProperty("mappingFields")
  private List<MappingFields> mappingFields;
}
