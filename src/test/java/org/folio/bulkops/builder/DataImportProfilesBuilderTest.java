package org.folio.bulkops.builder;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ActionProfile;
import org.folio.bulkops.domain.bean.MatchProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataImportProfilesBuilderTest extends BaseTest  {

  @Autowired
  private DataImportProfilesBuilder dataImportProfilesBuilder;

  @Test
  @SneakyThrows
  void shouldGetMatchProfile() {
    var matchProfile = dataImportProfilesBuilder.getMatchProfile();
    assertNotNull(matchProfile);
  }

  @Test
  @SneakyThrows
  void shouldGetActionProfile() {
    var actionProfile = dataImportProfilesBuilder.getActionProfile();
    assertNotNull(actionProfile);
  }

  @Test
  @SneakyThrows
  void shouldGetMappingProfile() {
    var mappingProfile = dataImportProfilesBuilder.getMappingProfile();
    assertNotNull(mappingProfile);
  }

  @Test
  @SneakyThrows
  void shouldGetJobProfilePost() {
    var actionProfile = ActionProfile.builder().id("action_profile_id").build();
    var matchProfile = MatchProfile.builder().id("match_profile_id").build();
    var jobProfilePost = dataImportProfilesBuilder.getJobProfilePost(matchProfile, actionProfile);
    assertNotNull(jobProfilePost);
    assertEquals("match_profile_id", jobProfilePost.getAddedRelations().get(0).getDetailProfileId());
    assertEquals("action_profile_id", jobProfilePost.getAddedRelations().get(1).getDetailProfileId());
  }
}
