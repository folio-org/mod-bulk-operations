package org.folio.bulkops.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.bulkops.domain.entity.Profile;
import org.folio.bulkops.domain.dto.ProfileUpdateRequest;
import org.folio.bulkops.mapper.ProfileRequestMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.folio.bulkops.domain.dto.ProfileRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.folio.bulkops.domain.dto.EntityType.USER;

@SpringBootTest(classes = {ProfileRequestMapperImpl.class, MappingMethods.class})
public class ProfileRequestMapperTest {
  @Autowired
  private ProfileRequestMapper profileRequestMapper;
  @Test
  void shouldMapProfileRequestToEntity() {
    UUID userId = UUID.randomUUID();
    String username = "creator-user";

    ProfileRequest request = new ProfileRequest()
      .name("Test Profile")
      .description("Description here")
      .locked(false)
      .entityType(USER);

    Profile entity = profileRequestMapper.toEntity(request, userId, username);

    assertThat(entity).isNotNull();
    assertThat(entity.getName()).isEqualTo(request.getName());
    assertThat(entity.getDescription()).isEqualTo(request.getDescription());
    assertThat(entity.getEntityType()).isEqualTo(request.getEntityType());
//    assertThat(entity.getLocked()).isEqualTo(request.getLocked());

    assertThat(entity.getCreatedBy()).isEqualTo(userId);
    assertThat(entity.getCreatedByUser()).isEqualTo(username);
    assertThat(entity.getUpdatedBy()).isEqualTo(userId);
    assertThat(entity.getUpdatedByUser()).isEqualTo(username);

    assertThat(entity.getCreatedDate()).isNotNull();
    assertThat(entity.getUpdatedDate()).isNotNull();
    assertThat(entity.getUpdatedDate()).isAfterOrEqualTo(entity.getCreatedDate());
  }

  @Test
  void shouldUpdateEntityFromUpdateRequest() {
    Profile existing = new Profile();
    existing.setId(UUID.randomUUID());
    existing.setName("Old Name");
    existing.setDescription("Old Desc");
    existing.setEntityType(USER);
    existing.setLocked(true);
//    existing.setCreatedDate(OffsetDateTime.now().minusDays(2));
    existing.setCreatedBy(UUID.randomUUID());
    existing.setCreatedByUser("original-user");

    ProfileUpdateRequest updateRequest = new ProfileUpdateRequest()
      .name("New Name")
      .description("New Desc")
      .locked(false)
      .entityType(USER);

    profileRequestMapper.updateEntity(existing, updateRequest);

    assertThat(existing.getName()).isEqualTo("New Name");
    assertThat(existing.getDescription()).isEqualTo("New Desc");
//    assertThat(existing.getLocked()).isFalse();
    assertThat(existing.getEntityType()).isEqualTo(USER);
  }
}
