package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@With
@Builder(toBuilder = true)
public class ConsortiumHolding {
  private String id;
  private String tenantId;
  private String instanceId;
}
