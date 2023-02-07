package org.folio.bulkops.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.OperationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bulk_operation")
public class BulkOperation {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
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

  private UUID dataExportJobId;
  private String linkToOriginFile;
  private String linkToModifiedFile;
  private String linkToResultFile;
  private Integer totalNumOfRecords;
  private Integer processedNumOfRecords;
  private Integer executionChunkSize;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String errorMessage;
}
