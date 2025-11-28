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
public class Progress {
  @JsonProperty("total")
  private Integer total = 0;

  @JsonProperty("processed")
  private Integer processed = 0;

  @JsonProperty("progress")
  private Integer progress = 0;

  @JsonProperty("success")
  private Integer success = 0;

  @JsonProperty("errors")
  private Integer errors = 0;

  @JsonProperty("warnings")
  private Integer warnings = 0;
}
