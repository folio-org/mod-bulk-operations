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
public class Subject {
  @JsonProperty("value")
  private String value;

  @JsonProperty("authorityId")
  private String authorityId;

  @JsonProperty("sourceId")
  private String sourceId;

  @JsonProperty("typeId")
  private String typeId;
}
