package org.folio.bulkops.repository;

import java.util.UUID;

import org.folio.bulkops.domain.entity.BulkOperationError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BulkOperationErrorRepository extends JpaRepository<BulkOperationError, UUID> {
  Page<BulkOperationError> findAllByBulkOperationId(UUID bulkOperationId, Pageable pageable);

}
