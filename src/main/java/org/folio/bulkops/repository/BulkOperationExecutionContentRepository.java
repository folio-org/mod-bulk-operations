package org.folio.bulkops.repository;

import java.util.Optional;
import java.util.UUID;

import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkOperationExecutionContentRepository extends JpaRepository<BulkOperationExecutionContent, UUID> {
  Page<BulkOperationExecutionContent> findByBulkOperationIdAndErrorMessageIsNotNullOrderByErrorType(UUID bulkOperationId, OffsetRequest offsetRequest);
  Page<BulkOperationExecutionContent> findByBulkOperationIdAndErrorMessageIsNotNullAndErrorTypeIsOrderByErrorType(UUID bulkOperationId, OffsetRequest offsetRequest, ErrorType errorType);
  Optional<BulkOperationExecutionContent> findFirstByBulkOperationIdAndIdentifier(UUID bulkOperationId, String identifier);
  int countAllByBulkOperationIdAndErrorMessageIsNotNullAndErrorTypeIs(UUID bulkOperationId, ErrorType errorType);
  @Query("SELECT COUNT(i) FROM BulkOperationExecutionContent i WHERE i.bulkOperationId = :bulkOperationId AND i.errorType = :errorType")
  long countByBulkOperationIdAndErrorType(UUID bulkOperationId, ErrorType errorType);

  @Query("SELECT COUNT(i) FROM BulkOperationExecutionContent i WHERE i.bulkOperationId = :bulkOperationId")
  long countByBulkOperationId(UUID bulkOperationId);
  void deleteByBulkOperationId(UUID bulkOperationId);
}
