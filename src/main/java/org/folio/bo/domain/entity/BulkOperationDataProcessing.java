package org.folio.bo.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.bo.domain.dto.StatusType;

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
@Table(name = "bulk_operation_data_processing")
public class BulkOperationDataProcessing {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID bulkOperationId;

  @Enumerated(EnumType.STRING)
  private StatusType status;

  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Integer totalNumOfRecords;
  private Integer processedNumOfRecords;
}
