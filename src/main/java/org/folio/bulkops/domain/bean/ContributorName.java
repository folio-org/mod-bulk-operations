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
public class ContributorName {

  @JsonProperty("name")
  private String name;

  @JsonProperty("contributorTypeId")
  private String contributorTypeId;

  @JsonProperty("contributorTypeText")
  private String contributorTypeText;

  @JsonProperty("contributorNameTypeId")
  private String contributorNameTypeId;

  @JsonProperty("authorityId")
  private String authorityId;

  @JsonProperty("primary")
  private Boolean primary;
}

