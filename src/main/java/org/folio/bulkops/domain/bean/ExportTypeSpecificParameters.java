package org.folio.bulkops.domain.bean;

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
public class ExportTypeSpecificParameters {
  @JsonProperty("bursarFeeFines")
  private BursarFeeFines bursarFeeFines;

  @JsonProperty("vendorEdiOrdersExportConfig")
  private VendorEdiOrdersExportConfig vendorEdiOrdersExportConfig;

  @JsonProperty("query")
  private String query;

  @JsonProperty("eHoldingsExportConfig")
  private EholdingsExportConfig eholdingsExportConfig;
}

