package org.folio.bulkops.repository;

import java.util.UUID;

import org.folio.bulkops.domain.entity.BulkOperationRuleDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkOperationRuleDetailsRepository extends JpaRepository<BulkOperationRuleDetails, UUID> {
  void deleteAllByRuleId(UUID ruleId);
}
