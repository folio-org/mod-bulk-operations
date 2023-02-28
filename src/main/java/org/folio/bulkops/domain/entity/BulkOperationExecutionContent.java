package org.folio.bulkops.domain.entity;

import java.util.UUID;

import org.folio.bulkops.domain.bean.StateType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "bulk_operation_execution_content")
public class BulkOperationExecutionContent {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private String identifier;
  private UUID bulkOperationExecutionChunkId;
  private UUID bulkOperationId;

  @Enumerated(EnumType.STRING)
  private StateType state;

  private String errorMessage;
}
