package org.folio.bulkops.domain.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

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
public class BursarFeeFines {
  @JsonProperty("daysOutstanding")
  private Integer daysOutstanding;

  @JsonProperty("patronGroups")
  @Valid
  private List<String> patronGroups = new ArrayList<>();

  @JsonProperty("servicePointId")
  private UUID servicePointId;

  @JsonProperty("feefineOwnerId")
  private UUID feefineOwnerId;

  @JsonProperty("transferAccountId")
  private UUID transferAccountId;

  @JsonProperty("typeMappings")
  @Valid
  private Map<String, List<BursarFeeFinesTypeMapping>> typeMappings = null;
}

