package org.folio.bulkops.domain.bean;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;
import lombok.With;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@With
@Builder(toBuilder = true)
public class ConsortiumHoldingCollection {

  @JsonProperty("holdings")
  private List<ConsortiumHolding> holdings;
}
