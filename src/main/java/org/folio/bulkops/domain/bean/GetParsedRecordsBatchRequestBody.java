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
public class GetParsedRecordsBatchRequestBody {
  @JsonProperty("conditions")
  private GetParsedRecordsBatchConditions conditions;
  @JsonProperty("recordType")
  private String recordType;
  @JsonProperty("includeDeleted")
  private boolean includeDeleted;
}
