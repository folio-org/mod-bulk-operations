package org.folio.bulkops.repository;

import java.util.List;
import java.util.UUID;

import org.folio.bulkops.domain.entity.BulkOperationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkOperationRuleRepository extends JpaRepository<BulkOperationRule, UUID> {
  List<BulkOperationRule> findAllByBulkOperationId(UUID bulkOperationId);

  void deleteAllByBulkOperationId(UUID bulkOperationId);
}
