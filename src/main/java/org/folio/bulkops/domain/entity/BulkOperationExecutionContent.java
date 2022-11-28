package org.folio.bulkops.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.dto.StateType;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bulk_operation_execution_content")
public class BulkOperationExecutionContent {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private String customIdentifier;
  private UUID bulkOperationExecutionChunkId;
  private UUID bulkOperationId;

  @Enumerated(EnumType.STRING)
  private StateType state;

  private String errorMessage;
}
