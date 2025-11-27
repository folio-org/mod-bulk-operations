package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
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
public class VendorEdiOrdersExportConfig {
  @JsonProperty("exportConfigId")
  private UUID exportConfigId;

  @JsonProperty("vendorId")
  private UUID vendorId;

  @JsonProperty("configName")
  private String configName;

  @JsonProperty("configDescription")
  private String configDescription;

  @JsonProperty("ediConfig")
  private EdiConfig ediConfig;

  @JsonProperty("ediFtp")
  private EdiFtp ediFtp;

  @JsonProperty("ediSchedule")
  private EdiSchedule ediSchedule;

  @JsonProperty("isDefaultConfig")
  private Boolean isDefaultConfig = false;

  @JsonProperty("integrationType")
  private String integrationType;

  @JsonProperty("transmissionMethod")
  private String transmissionMethod;

  @JsonProperty("fileFormat")
  private String fileFormat;
}
