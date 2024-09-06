package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.Builder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@With
@Builder(toBuilder = true)
public class ConsortiumItem {
  private String id;
  private String tenantId;
  private String instanceId;
  private String holdingsRecordId;
}
