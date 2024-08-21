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
public class MatchDetail {

  @JsonProperty("incomingRecordType")
  private String incomingRecordType;

  @JsonProperty("existingRecordType")
  private String existingRecordType;

  @JsonProperty("incomingMatchExpression")
  private MatchExpression incomingMatchExpression;

  @JsonProperty("matchCriterion")
  private String matchCriterion;

  @JsonProperty("existingMatchExpression")
  private MatchExpression existingMatchExpression;
}
