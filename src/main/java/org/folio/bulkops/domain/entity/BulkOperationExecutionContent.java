package org.folio.bulkops.domain.entity;

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
