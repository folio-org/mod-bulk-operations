package org.folio.bulkops.repository;

import org.folio.bulkops.domain.entity.BulkOperationRuleDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BulkOperationRuleDetailsRepository extends JpaRepository<BulkOperationRuleDetails, UUID> {
}
