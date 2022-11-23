package org.folio.bo.repository;

import org.folio.bo.domain.entity.BulkOperationExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BulkOperationExecutionRepository extends JpaRepository<BulkOperationExecution, UUID> {
}
