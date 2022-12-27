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
public class StatisticalCode   {
  @JsonProperty("id")
  private String id;

  @JsonProperty("code")
  private String code;

  @JsonProperty("name")
  private String name;

  @JsonProperty("statisticalCodeTypeId")
  private String statisticalCodeTypeId;

  @JsonProperty("source")
  private String source;

  @JsonProperty("metadata")
  private Metadata metadata;
}

