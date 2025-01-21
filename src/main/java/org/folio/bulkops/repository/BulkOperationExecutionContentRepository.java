package org.folio.bulkops.repository;

import java.util.Optional;
import java.util.UUID;

import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkOperationExecutionContentRepository extends JpaRepository<BulkOperationExecutionContent, UUID> {
  Page<BulkOperationExecutionContent> findByBulkOperationIdAndErrorMessageIsNotNull(UUID bulkOperationId, OffsetRequest offsetRequest);
  Optional<BulkOperationExecutionContent> findFirstByBulkOperationIdAndIdentifierAndErrorMessage(UUID bulkOperationId, String identifier, String errorMessage);
  int countAllByBulkOperationIdAndErrorMessageIsNotNullAndErrorTypeIs(UUID bulkOperationId, ErrorType errorType);

  void deleteByBulkOperationId(UUID bulkOperationId);
}
