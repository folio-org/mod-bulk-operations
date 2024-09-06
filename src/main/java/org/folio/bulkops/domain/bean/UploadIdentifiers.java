package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UploadIdentifiers {

  @JsonProperty("identifierType")
  private String identifierType;

  @JsonProperty("identifierValues")
  private List<UUID> identifierValues;
}
