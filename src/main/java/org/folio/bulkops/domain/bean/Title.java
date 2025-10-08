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
public class Title {
  @JsonProperty("briefHoldingsRecord")
  private BriefHoldingsRecord briefHoldingsRecord;

  @JsonProperty("briefInstance")
  private BriefInstance briefInstance;
}

