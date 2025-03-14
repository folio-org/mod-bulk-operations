package org.folio.bulkops.domain.bean;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
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
public class Job {
  @JsonProperty("id")
  private UUID id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("description")
  private String description;

  @JsonProperty("source")
  private String source;

  @JsonProperty("isSystemSource")
  private Boolean isSystemSource;

  @JsonProperty("tenant")
  private String tenant;

  @JsonProperty("type")
  private ExportType type;

  @JsonProperty("exportTypeSpecificParameters")
  private ExportTypeSpecificParameters exportTypeSpecificParameters;

  @JsonProperty("status")
  private JobStatus status;

  @JsonProperty("files")
  @Valid
  private List<String> files = null;

  @JsonProperty("fileNames")
  @Valid
  private List<String> fileNames = null;

  @JsonProperty("startTime")
  @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
  private Date startTime;

  @JsonProperty("endTime")
  @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
  private Date endTime;

  @JsonProperty("metadata")
  private Metadata metadata;

  @JsonProperty("outputFormat")
  private String outputFormat;

  @JsonProperty("errorDetails")
  private String errorDetails;

  @JsonProperty("batchStatus")
  private BatchStatus batchStatus;

  @JsonProperty("identifierType")
  private IdentifierType identifierType;

  @JsonProperty("entityType")
  private EntityType entityType;

  @JsonProperty("progress")
  private Progress progress;
}

