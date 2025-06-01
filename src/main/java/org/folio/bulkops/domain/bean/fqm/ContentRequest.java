package org.folio.bulkops.domain.bean.fqm;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
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
public class ContentRequest {

  @JsonProperty("entityTypeId")
  private UUID entityTypeId;
  @JsonProperty("fields")
  @Valid
  private List<String> fields = new ArrayList<>();
  @JsonProperty("ids")
  @Valid
  private List<UUID> ids = new ArrayList<>();
  @JsonProperty("localize")
  private Boolean localize = false;
  @JsonProperty("userId")
  private UUID userId;

}
