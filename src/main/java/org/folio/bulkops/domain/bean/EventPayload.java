package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.ProfileInfo;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EventPayload {

  @JsonProperty("parentJobId")
  private UUID parentJobId;

  @JsonProperty("jobProfileInfo")
  private ProfileInfo jobProfileInfo;
}
