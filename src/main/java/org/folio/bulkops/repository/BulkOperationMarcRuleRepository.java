package org.folio.bulkops.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.bulkops.domain.entity.BulkOperationMarcRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkOperationMarcRuleRepository
    extends JpaRepository<BulkOperationMarcRule, UUID> {

  List<BulkOperationMarcRule> findAllByBulkOperationId(UUID bulkOperationId);

  Optional<BulkOperationMarcRule> findFirstByBulkOperationId(UUID bulkOperationId);

  void deleteAllByBulkOperationId(UUID bulkOperationId);
}
