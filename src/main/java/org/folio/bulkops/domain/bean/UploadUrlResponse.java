package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlResponse {
  @JsonProperty("url")
  private String url;

  @JsonProperty("key")
  private String key;

  @JsonProperty("uploadId")
  private String uploadId;
}
