package org.folio.bulkops.repository;

import java.util.UUID;
import org.folio.bulkops.domain.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
}
