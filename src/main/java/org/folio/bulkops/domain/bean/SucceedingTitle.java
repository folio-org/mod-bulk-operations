package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SucceedingTitle {
  @JsonProperty("id")
  private UUID id;

  @JsonProperty("succeedingInstanceId")
  private UUID succeedingInstanceId;

  @JsonProperty("title")
  private String title;

  @JsonProperty("hrid")
  private String hrid;

  @JsonProperty("identifiers")
  private List<Identifier> identifiers;
}
