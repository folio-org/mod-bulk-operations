package org.folio.bulkops.builder;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ActionProfile;
import org.folio.bulkops.domain.bean.MappingProfile;
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
  void shouldGetActionProfilePostToUpdateInstance() {
    var mappingProfileUpdateInstance = MappingProfile.builder().id("mapping_profile_update_instance_id").build();
    var actionProfilePost = dataImportProfilesBuilder.getActionProfilePostToUpdateInstance(mappingProfileUpdateInstance);
    assertNotNull(actionProfilePost);
    assertEquals("mapping_profile_update_instance_id", actionProfilePost.getAddedRelations().get(0).getDetailProfileId());
  }

  @Test
  @SneakyThrows
  void shouldGetActionProfilePostToUpdateSrs() {
    var mappingProfileUpdateSrs = MappingProfile.builder().id("mapping_profile_update_srs_id").build();
    var actionProfilePost = dataImportProfilesBuilder.getActionProfilePostToUpdateSrs(mappingProfileUpdateSrs);
    assertNotNull(actionProfilePost);
    assertEquals("mapping_profile_update_srs_id", actionProfilePost.getAddedRelations().get(0).getDetailProfileId());
  }

  @Test
  @SneakyThrows
  void shouldGetMappingProfileToUpdateSrs() {
    var mappingProfile = dataImportProfilesBuilder.getMappingProfileToUpdateSrs();
    assertNotNull(mappingProfile);
  }

  @Test
  @SneakyThrows
  void shouldGetMappingProfileToUpdateInstance() {
    var mappingProfile = dataImportProfilesBuilder.getMappingProfileToUpdateInstance();
    assertNotNull(mappingProfile);
  }

  @Test
  @SneakyThrows
  void shouldGetJobProfilePost() {
    var actionProfileUpdateSrs = ActionProfile.builder().id("action_profile_update_srs_id").build();
    var matchProfile = MatchProfile.builder().id("match_profile_id").build();
    var jobProfilePost = dataImportProfilesBuilder.getJobProfilePost(matchProfile, actionProfileUpdateSrs);
    assertNotNull(jobProfilePost);
    assertEquals("match_profile_id", jobProfilePost.getAddedRelations().get(0).getDetailProfileId());
    assertEquals("action_profile_update_srs_id", jobProfilePost.getAddedRelations().get(1).getDetailProfileId());
    assertEquals("match_profile_id", jobProfilePost.getAddedRelations().get(1).getMasterProfileId());

  }
}
