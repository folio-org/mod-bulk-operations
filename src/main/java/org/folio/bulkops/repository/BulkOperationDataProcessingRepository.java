package org.folio.bulkops.repository;

import java.util.UUID;

import org.folio.bulkops.domain.entity.BulkOperationDataProcessing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkOperationDataProcessingRepository extends JpaRepository<BulkOperationDataProcessing, UUID> {
}
