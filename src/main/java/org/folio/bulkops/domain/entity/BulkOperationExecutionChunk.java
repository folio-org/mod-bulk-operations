package org.folio.bulkops.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.folio.bulkops.domain.dto.StateType;

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
@Table(name = "bulk_operation_execution_chunk")
public class BulkOperationExecutionChunk {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID bulkOperationExecutionId;
  private UUID bulkOperationId;
  private Integer firstRecordIndex;
  private Integer lastRecordIndex;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  @Enumerated(EnumType.STRING)
  private StateType state;

  private String errorMessage;
}
