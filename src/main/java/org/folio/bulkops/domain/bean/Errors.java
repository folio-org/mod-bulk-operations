package org.folio.bulkops.domain.bean;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
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
public class Errors   {
  @JsonProperty("errors")
  @Valid
  private List<Error> errors = null;

  @JsonProperty("total_records")
  private Integer totalRecords;
}

