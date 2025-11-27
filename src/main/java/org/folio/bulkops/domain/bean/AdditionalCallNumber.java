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
public class AdditionalCallNumber {
  @JsonProperty("typeId")
  private String typeId;

  @JsonProperty("prefix")
  private String prefix;

  @JsonProperty("callNumber")
  private String callNumber;

  @JsonProperty("suffix")
  private String suffix;
}
