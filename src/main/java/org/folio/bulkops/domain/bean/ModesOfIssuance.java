package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.ArrayList;
import java.util.List;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ModesOfIssuance {
  @JsonProperty("issuanceModes")
  @Valid
  private List<ModeOfIssuance> modes = new ArrayList<>();

  @JsonProperty("totalRecords")
  private Integer totalRecords;
}

