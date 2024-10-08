package org.folio.bulkops.domain.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.ArrayList;
import java.util.List;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileDefinition {
  @JsonProperty("id")
  private String id;

  @JsonProperty("metaJobExecutionId")
  private String metaJobExecutionId;

  @JsonProperty("fileDefinitions")
  private List<FileDefinition> fileDefinitions = new ArrayList<>();

  @JsonProperty("metadata")
  private Metadata metadata;
}
