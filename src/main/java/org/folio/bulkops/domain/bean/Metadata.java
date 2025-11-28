package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
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
public class Metadata {
  @JsonProperty("createdDate")
  @org.springframework.format.annotation.DateTimeFormat(
      iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
  private Date createdDate;

  @JsonProperty("createdByUserId")
  private UUID createdByUserId;

  @JsonProperty("createdByUsername")
  private String createdByUsername;

  @JsonProperty("updatedDate")
  @org.springframework.format.annotation.DateTimeFormat(
      iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
  private Date updatedDate;

  @JsonProperty("updatedByUserId")
  private UUID updatedByUserId;

  @JsonProperty("updatedByUsername")
  private String updatedByUsername;
}
