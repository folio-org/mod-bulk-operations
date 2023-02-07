package org.folio.bulkops.domain.bean;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

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
public class ServicePoints   {
  @JsonProperty("servicepoints")
  @Valid
  private List<ServicePoint> servicepoints = new ArrayList<>();

  @JsonProperty("totalRecords")
  private Integer totalRecords;
}

