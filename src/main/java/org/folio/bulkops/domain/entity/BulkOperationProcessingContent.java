package org.folio.bulkops.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bulkops.domain.bean.StateType;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bulk_operation_processing_content")
public class BulkOperationProcessingContent {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private String identifier;
  private UUID bulkOperationId;

  @Enumerated(EnumType.STRING)
  private StateType state;

  private String errorMessage;
}
