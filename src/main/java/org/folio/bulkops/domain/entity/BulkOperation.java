package org.folio.bulkops.domain.entity;

import static java.util.Objects.isNull;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import lombok.AccessLevel;
import lombok.Setter;
import org.folio.bulkops.domain.converter.PostgresUUIDConverter;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.OperationType;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bulk_operation")
public class BulkOperation {
  @Id
  @Convert(converter = PostgresUUIDConverter.class)
  private UUID id;

  @Column(insertable = false, updatable = false)
  private int hrId;

  private UUID userId;

  @Enumerated(EnumType.STRING)
  private OperationType operationType;

  @Enumerated(EnumType.STRING)
  private EntityType entityType;

  @Enumerated(EnumType.STRING)
  private IdentifierType identifierType;

  @Enumerated(EnumType.STRING)
  private OperationStatusType status;

  @Enumerated(EnumType.STRING)
  private ApproachType approach;

  private UUID dataExportJobId;
  private String linkToTriggeringCsvFile;

  private String linkToMatchedRecordsJsonFile;
  private String linkToMatchedRecordsCsvFile;
  private String linkToMatchedRecordsErrorsCsvFile;
  private String linkToModifiedRecordsJsonFile;
  private String linkToModifiedRecordsCsvFile;
  private String linkToPreviewRecordsJsonFile;
  private String linkToCommittedRecordsJsonFile;
  private String linkToCommittedRecordsCsvFile;
  private String linkToCommittedRecordsErrorsCsvFile;

  @Setter(AccessLevel.NONE)
  private int totalNumOfRecords;
  @Setter(AccessLevel.NONE)
  private int processedNumOfRecords;
  @Setter(AccessLevel.NONE)
  private int executionChunkSize;
  @Setter(AccessLevel.NONE)
  private int matchedNumOfRecords;
  @Setter(AccessLevel.NONE)
  private int committedNumOfRecords;
  @Setter(AccessLevel.NONE)
  private int matchedNumOfErrors;
  @Setter(AccessLevel.NONE)
  private int committedNumOfErrors;

  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String errorMessage;

  public void setTotalNumOfRecords(Integer totalNumOfRecords) {
    this.totalNumOfRecords = isNull(totalNumOfRecords) ? 0 : totalNumOfRecords;
  }

  public void setProcessedNumOfRecords(Integer processedNumOfRecords) {
    this.processedNumOfRecords = isNull(processedNumOfRecords) ? 0 : processedNumOfRecords;
  }

  public void setExecutionChunkSize(Integer executionChunkSize) {
    this.executionChunkSize = isNull(executionChunkSize) ? 0 : executionChunkSize;
  }

  public void setMatchedNumOfRecords(Integer matchedNumOfRecords) {
    this.matchedNumOfRecords = isNull(matchedNumOfRecords) ? 0 : matchedNumOfRecords;
  }

  public void setCommittedNumOfRecords(Integer committedNumOfRecords) {
    this.committedNumOfRecords = isNull(committedNumOfRecords) ? 0 : committedNumOfRecords;
  }

  public void setMatchedNumOfErrors(Integer matchedNumOfErrors) {
    this.matchedNumOfErrors = isNull(matchedNumOfErrors) ? 0 : matchedNumOfErrors;
  }

  public void setCommittedNumOfErrors(Integer committedNumOfErrors) {
    this.committedNumOfErrors = isNull(committedNumOfErrors) ? 0 : committedNumOfErrors;
  }
}
