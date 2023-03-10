package org.folio.bulkops.domain.bean;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
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
public class IllPolicyCollection {
  @JsonProperty("illPolicies")
  @Valid
  private List<IllPolicy> illPolicies = new ArrayList<>();

  @JsonProperty("totalRecords")
  private Integer totalRecords;
}

