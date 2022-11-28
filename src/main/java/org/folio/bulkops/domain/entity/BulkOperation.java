package org.folio.bulkops.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.EntityCustomIdentifierType;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.OperationStatusType;
import org.folio.bulkops.domain.dto.OperationType;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
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
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID userId;

  @Enumerated(EnumType.STRING)
  private OperationType operationType;

  @Enumerated(EnumType.STRING)
  private EntityType entityType;

  @Enumerated(EnumType.STRING)
  private EntityCustomIdentifierType entityCustomIdentifierType;

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
