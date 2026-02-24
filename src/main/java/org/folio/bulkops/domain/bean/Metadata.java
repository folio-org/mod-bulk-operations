package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.format.CustomDateSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {
  @JsonProperty("createdDate")
  @JsonSerialize(using = CustomDateSerializer.class)
  private Date createdDate;

  @JsonProperty("createdByUserId")
  private UUID createdByUserId;

  @JsonProperty("createdByUsername")
  private String createdByUsername;

  @JsonProperty("updatedDate")
  @JsonSerialize(using = CustomDateSerializer.class)
  private Date updatedDate;

  @JsonProperty("updatedByUserId")
  private UUID updatedByUserId;

  @JsonProperty("updatedByUsername")
  private String updatedByUsername;
}
