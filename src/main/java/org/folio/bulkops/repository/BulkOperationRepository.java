package org.folio.bulkops.repository;

import java.util.Optional;
import java.util.UUID;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BulkOperationRepository extends JpaRepository<BulkOperation, UUID> {
  Optional<BulkOperation> findByDataImportJobProfileId(UUID profileId);

  @Modifying
  @Transactional
  @Query(
      "UPDATE bulk_operation b SET b.processedNumOfRecords = :processed, "
          + "b.matchedNumOfRecords = :matched WHERE b.id = :id")
  void updateExecutionCounters(
      @Param("id") UUID id, @Param("processed") int processed, @Param("matched") int matched);
}
