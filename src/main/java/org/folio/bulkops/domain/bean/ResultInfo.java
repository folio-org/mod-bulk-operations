package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
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
public class ResultInfo {
  @JsonProperty("totalRecords")
  private Integer totalRecords;

  @JsonProperty("responseTime")
  private BigDecimal responseTime;

  @JsonProperty("facets")
  @Valid
  private List<Facet> facets = null;

  @JsonProperty("diagnostics")
  @Valid
  private List<Diagnostic> diagnostics = null;
}
