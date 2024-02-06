package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.List;
import java.util.UUID;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PrecedingTitle {
  @JsonProperty("id")
  private UUID id;

  @JsonProperty("precedingInstanceId")
  private UUID precedingInstanceId;

  @JsonProperty("title")
  private String title;

  @JsonProperty("hrid")
  private String hrid;

  @JsonProperty("identifiers")
  private List<Identifier> identifiers;
}

