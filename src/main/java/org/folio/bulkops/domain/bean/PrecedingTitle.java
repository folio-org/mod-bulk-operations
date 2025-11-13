package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class PrecedingTitle {
  @JsonProperty("id")
  private UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("precedingInstanceId")
  private UUID precedingInstanceId;

  @JsonProperty("title")
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("hrid")
  private String hrid;

  @JsonProperty("identifiers")
  private List<Identifier> identifiers;
}

