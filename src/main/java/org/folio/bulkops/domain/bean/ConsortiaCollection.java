package org.folio.bulkops.domain.bean;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsortiaCollection {

  @JsonProperty("consortia")
  private List<Consortia> consortia = new ArrayList<>();
}
