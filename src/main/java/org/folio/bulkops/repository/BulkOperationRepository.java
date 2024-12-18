package org.folio.bulkops.repository;

import java.util.Optional;
import java.util.UUID;

import org.folio.bulkops.domain.entity.BulkOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkOperationRepository extends JpaRepository<BulkOperation, UUID> {
  Optional<BulkOperation> findByDataExportJobId(UUID jobId);
  Optional<BulkOperation> findByDataImportJobProfileId(UUID profileId);
}
