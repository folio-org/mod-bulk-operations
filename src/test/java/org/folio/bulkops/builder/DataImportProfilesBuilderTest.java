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
  void shouldGetActionProfileToUpdateInstance() {
    var actionProfile = dataImportProfilesBuilder.getActionProfileToUpdateInstance();
    assertNotNull(actionProfile);
  }

  @Test
  @SneakyThrows
  void shouldGetActionProfileToUpdateSrs() {
    var actionProfile = dataImportProfilesBuilder.getActionProfileToUpdateSrs();
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
    var actionProfileUpdateInstance = ActionProfile.builder().id("action_profile_update_instance_id").build();
    var actionProfileUpdateSrs = ActionProfile.builder().id("action_profile_update_srs_id").build();
    var matchProfile = MatchProfile.builder().id("match_profile_id").build();
    var jobProfilePost = dataImportProfilesBuilder.getJobProfilePost(matchProfile, actionProfileUpdateInstance, actionProfileUpdateSrs);
    assertNotNull(jobProfilePost);
    assertEquals("match_profile_id", jobProfilePost.getAddedRelations().get(0).getDetailProfileId());
    assertEquals("action_profile_update_instance_id", jobProfilePost.getAddedRelations().get(1).getDetailProfileId());
    assertEquals("match_profile_id", jobProfilePost.getAddedRelations().get(1).getMasterProfileId());
    assertEquals("action_profile_update_srs_id", jobProfilePost.getAddedRelations().get(2).getDetailProfileId());
    assertEquals("match_profile_id", jobProfilePost.getAddedRelations().get(2).getMasterProfileId());
  }
}
