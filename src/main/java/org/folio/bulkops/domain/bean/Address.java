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
public class Address {
  @JsonProperty("id")
  private String id;

  @JsonProperty("countryId")
  private String countryId;

  @JsonProperty("addressLine1")
  private String addressLine1;

  @JsonProperty("addressLine2")
  private String addressLine2;

  @JsonProperty("city")
  private String city;

  @JsonProperty("region")
  private String region;

  @JsonProperty("postalCode")
  private String postalCode;

  @JsonProperty("addressTypeId")
  private String addressTypeId;

  @JsonProperty("primaryAddress")
  private Boolean primaryAddress;
}
