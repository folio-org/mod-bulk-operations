package org.folio.bulkops.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.OperationType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bulk_operation")
public class BulkOperation {
  @Id
  private UUID id;

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

  private Integer totalNumOfRecords;
  private Integer processedNumOfRecords;
  private Integer executionChunkSize;
  private Integer matchedNumOfRecords;
  private Integer committedNumOfRecords;
  private Integer matchedNumOfErrors;
  private Integer committedNumOfErrors;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String errorMessage;
}
