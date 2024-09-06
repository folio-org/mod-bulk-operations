package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@With
@Builder(toBuilder = true)
public class ConsortiumItemCollection {

  @JsonProperty("items")
  private List<ConsortiumItem> items = new ArrayList<>();
}
