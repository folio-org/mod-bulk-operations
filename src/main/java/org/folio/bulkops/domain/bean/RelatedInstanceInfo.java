package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.List;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RelatedInstanceInfo {
  @JsonProperty("actionStatus")
  private String actionStatus;

  @JsonProperty("idList")
  private List<String> idList;

  @JsonProperty("hridList")
  private List<String> hridList;

  @JsonProperty("error")
  private String error;
}
