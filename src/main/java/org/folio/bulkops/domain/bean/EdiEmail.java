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
public class EdiEmail {
  @JsonProperty("emailFrom")
  private String emailFrom;

  @JsonProperty("emailTo")
  private String emailTo;

  @JsonProperty("isPrimaryTransmissionMethod")
  private Boolean isPrimaryTransmissionMethod;
}
