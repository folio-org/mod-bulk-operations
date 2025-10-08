package org.folio.bulkops.repository;

import java.util.UUID;
import org.folio.bulkops.domain.entity.BulkOperationProcessingContent;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkOperationProcessingContentRepository
        extends JpaRepository<BulkOperationProcessingContent, UUID> {
  Page<BulkOperationProcessingContent> findByBulkOperationIdAndErrorMessageIsNotNull(
          UUID bulkOperationId, OffsetRequest offsetRequest);
}
