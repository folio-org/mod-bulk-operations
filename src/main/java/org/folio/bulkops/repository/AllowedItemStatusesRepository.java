package org.folio.bulkops.repository;

import org.folio.bulkops.domain.entity.AllowedItemStatuses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AllowedItemStatusesRepository extends JpaRepository<AllowedItemStatuses, String> {
  Optional<AllowedItemStatuses> findByStatus(String status);
}
