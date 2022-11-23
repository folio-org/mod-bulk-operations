package org.folio.bo.repository;

import org.folio.bo.domain.entity.BulkOperationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BulkOperationRuleRepository extends JpaRepository<BulkOperationRule, UUID> {
}
