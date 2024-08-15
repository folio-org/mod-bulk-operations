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

  @JsonProperty("id")
  private String id;

  @JsonProperty("masterProfileId")
  private String masterProfileId;

  @JsonProperty("detailProfileId")
  private String detailProfileId;

  @JsonProperty("order")
  private Integer order;

  @JsonProperty("reactTo")
  private String reactTo;

  @JsonProperty("triggered")
  private Boolean triggered;

  @JsonProperty("masterProfileType")
  private String masterProfileType;

  @JsonProperty("detailProfileType")
  private String detailProfileType;

  @JsonProperty("masterWrapperId")
  private String masterWrapperId;

  @JsonProperty("detailWrapperId")
  private String detailWrapperId;

  @JsonProperty("jobProfileId")
  private String jobProfileId;
}
