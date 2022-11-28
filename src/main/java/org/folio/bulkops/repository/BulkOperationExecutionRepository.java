package org.folio.bulkops.repository;

import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BulkOperationExecutionRepository extends JpaRepository<BulkOperationExecution, UUID> {
}
