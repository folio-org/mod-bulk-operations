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
public class JobExecution {

  @JsonProperty("id")
  private String id;

  @JsonProperty("hrid")
  private Integer hrid;

  @JsonProperty("fileName")
  private String fileName;

  @JsonProperty("status")
  private String status;
}
