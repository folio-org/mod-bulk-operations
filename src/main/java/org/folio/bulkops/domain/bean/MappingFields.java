package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.HashMap;

@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MappingFields {

  @JsonProperty("name")
  private String name;

  @JsonProperty("enabled")
  private String enabled;

  @JsonProperty("required")
  private Boolean required;

  @JsonProperty("path")
  private String path;

  @JsonProperty("acceptedValues")
  private HashMap<String, String> acceptedValues;
}
