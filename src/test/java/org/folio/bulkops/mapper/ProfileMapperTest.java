package org.folio.bulkops.mapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.SneakyThrows;
import org.folio.bulkops.domain.entity.Profile;
import org.folio.bulkops.domain.dto.ProfileDto;
import org.folio.bulkops.domain.dto.ProfileSummaryDTO;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.junit.jupiter.api.Test;
import org.folio.bulkops.mapper.ProfileMapperImpl;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {ProfileMapperImpl.class, MappingMethods.class})
class ProfileMapperTest {

  @Autowired
  private ProfileMapper profileMapper;

  @SneakyThrows
  @Test
  void shouldMapProfileToProfileDto() {
    Profile profile = new Profile();
    profile.setId(UUID.randomUUID());
    profile.setName("Sample Profile");
    profile.setDescription("Sample description for the profile");
    profile.setLocked(false);
    profile.setEntityType(EntityType.USER);
    profile.setBulkOperationRuleCollection(new BulkOperationRuleCollection());
    profile.setBulkOperationMarcRuleCollection(new BulkOperationMarcRuleCollection());
    profile.setCreatedDate(OffsetDateTime.now().minusDays(1));
    profile.setCreatedBy(UUID.randomUUID());
    profile.setCreatedByUser("creator-user");
    profile.setUpdatedDate(OffsetDateTime.now());
    profile.setUpdatedBy(UUID.randomUUID());
    profile.setUpdatedByUser("updater-user");

    ProfileDto dto = profileMapper.toDto(profile);

    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(profile.getId());
    assertThat(dto.getName()).isEqualTo(profile.getName());
    assertThat(dto.getDescription()).isEqualTo(profile.getDescription());
    assertThat(dto.getEntityType()).isEqualTo(profile.getEntityType());
    assertThat(dto.getBulkOperationRuleCollection()).isEqualTo(profile.getBulkOperationRuleCollection());
    assertThat(dto.getBulkOperationMarcRuleCollection()).isEqualTo(profile.getBulkOperationMarcRuleCollection());
    assertThat(dto.getCreatedBy()).isEqualTo(profile.getCreatedBy());
    assertThat(dto.getCreatedByUser()).isEqualTo(profile.getCreatedByUser());
    assertThat(dto.getUpdatedBy()).isEqualTo(profile.getUpdatedBy());
    assertThat(dto.getUpdatedByUser()).isEqualTo(profile.getUpdatedByUser());
  }

  @Test
  void shouldMapProfileToSummaryDto() {
    UUID profileId = UUID.randomUUID();
    Profile profile = new Profile();
    profile.setId(profileId);
    profile.setName("Summary Profile");
    profile.setDescription("Summary Desc");
    profile.setLocked(true);
    profile.setEntityType(EntityType.USER);
    profile.setCreatedBy(UUID.randomUUID());
    profile.setCreatedByUser("summary-user");
    profile.setCreatedDate(OffsetDateTime.now().minusDays(2));
    profile.setUpdatedBy(UUID.randomUUID());
    profile.setUpdatedByUser("update-user");
    profile.setUpdatedDate(OffsetDateTime.now());

    ProfileSummaryDTO summary = profileMapper.toSummaryDTO(profile);

    assertThat(summary).isNotNull();
    assertThat(summary.getId()).isEqualTo(profile.getId());
    assertThat(summary.getName()).isEqualTo(profile.getName());
    assertThat(summary.getDescription()).isEqualTo(profile.getDescription());
    assertThat(summary.getEntityType()).isEqualTo(profile.getEntityType());
    assertThat(summary.getCreatedBy()).isEqualTo(profile.getCreatedBy());
    assertThat(summary.getCreatedByUser()).isEqualTo(profile.getCreatedByUser());
    assertThat(summary.getUpdatedBy()).isEqualTo(profile.getUpdatedBy());
    assertThat(summary.getUpdatedByUser()).isEqualTo(profile.getUpdatedByUser());
  }
}
