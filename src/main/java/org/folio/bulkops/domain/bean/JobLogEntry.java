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
public class JobLogEntry {

  @JsonProperty("error")
  private String error;

  @JsonProperty("sourceRecordId")
  private String sourceRecordId;

  @JsonProperty("sourceRecordActionStatus")
  private ActionStatus sourceRecordActionStatus;

  @JsonProperty("sourceRecordType")
  private String sourceRecordType;

  @JsonProperty("relatedInstanceInfo")
  private RelatedInstanceInfo relatedInstanceInfo;

  public enum ActionStatus {
    CREATED,
    UPDATED,
    MULTIPLE,
    DISCARDED
  }
}
