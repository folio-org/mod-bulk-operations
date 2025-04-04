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
public class Dates {

  @JsonProperty("dateTypeId")
  private String dateTypeId;

  @JsonProperty("date1")
  private String date1;

  @JsonProperty("date2")
  private String date2;
}

