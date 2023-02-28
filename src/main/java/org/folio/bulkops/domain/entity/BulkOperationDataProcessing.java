package org.folio.bulkops.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.folio.bulkops.domain.bean.StatusType;

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
