package org.folio.bulkops.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkOperationExecutionRepository
    extends JpaRepository<BulkOperationExecution, UUID> {

  Optional<BulkOperationExecution> findByBulkOperationId(UUID uuid);

  List<BulkOperationExecution> findAllByBulkOperationId(UUID uuid);
}
