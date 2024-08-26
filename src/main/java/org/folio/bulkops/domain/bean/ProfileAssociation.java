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
public class ProfileAssociation {

  @JsonProperty("masterProfileId")
  private String masterProfileId;

  @JsonProperty("masterWrapperId")
  private String masterWrapperId;

  @JsonProperty("masterProfileType")
  private String masterProfileType;

  @JsonProperty("detailProfileId")
  private String detailProfileId;

  @JsonProperty("detailProfileType")
  private String detailProfileType;

  @JsonProperty("detailWrapperId")
  private String detailWrapperId;

  @JsonProperty("order")
  private Integer order;

  @JsonProperty("reactTo")
  private String reactTo;
}
