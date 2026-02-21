package org.folio.bulkops.builder;

import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.ActionProfile;
import org.folio.bulkops.domain.bean.ActionProfilePost;
import org.folio.bulkops.domain.bean.JobProfilePost;
import org.folio.bulkops.domain.bean.MappingProfile;
import org.folio.bulkops.domain.bean.MatchProfile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Log4j2
public class DataImportProfilesBuilder {

  private static final String DATA_IMPORT_PROFILES_PATH = "/import/profiles/";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public MatchProfile getMatchProfile() throws IOException {
    String path = DATA_IMPORT_PROFILES_PATH + "match_profile.json";
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(path)) {
      return OBJECT_MAPPER.readValue(is, MatchProfile.class);
    }
  }

  @SuppressWarnings("unused")
  public ActionProfilePost getActionProfilePostToUpdateInstance(
      MappingProfile mappingProfileToUpdateInstance) throws IOException {
    String path = DATA_IMPORT_PROFILES_PATH + "action_update_instance_profile_post.json";
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(path)) {
      var actionProfile = OBJECT_MAPPER.readValue(is, ActionProfilePost.class);
      var addedRelations = actionProfile.getAddedRelations();
      addedRelations.stream()
          .findFirst()
          .ifPresent(r -> r.setDetailProfileId(mappingProfileToUpdateInstance.getId()));
      return actionProfile;
    }
  }

  public ActionProfilePost getActionProfilePostToUpdateSrs(MappingProfile mappingProfileToUpdateSrs)
      throws IOException {
    String path = DATA_IMPORT_PROFILES_PATH + "action_update_srs_profile_post.json";
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(path)) {
      var actionProfile = OBJECT_MAPPER.readValue(is, ActionProfilePost.class);
      var addedRelations = actionProfile.getAddedRelations();
      addedRelations.stream()
          .findFirst()
          .ifPresent(r -> r.setDetailProfileId(mappingProfileToUpdateSrs.getId()));
      return actionProfile;
    }
  }

  @SuppressWarnings("unused")
  public MappingProfile getMappingProfileToUpdateInstance() throws IOException {
    String path = DATA_IMPORT_PROFILES_PATH + "mapping_profile_update_instance.json";
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(path)) {
      return OBJECT_MAPPER.readValue(is, MappingProfile.class);
    }
  }

  public MappingProfile getMappingProfileToUpdateSrs() throws IOException {
    String path = DATA_IMPORT_PROFILES_PATH + "mapping_profile_update_srs.json";
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(path)) {
      return OBJECT_MAPPER.readValue(is, MappingProfile.class);
    }
  }

  public JobProfilePost getJobProfilePost(
      MatchProfile matchProfile, ActionProfile actionProfileToUpdateSrs) throws IOException {
    String path = DATA_IMPORT_PROFILES_PATH + "job_profile_post.json";
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(path)) {
      var jobProfilePost = OBJECT_MAPPER.readValue(is, JobProfilePost.class);
      var addedRelations = jobProfilePost.getAddedRelations();
      addedRelations.stream()
          .findFirst()
          .ifPresent(r -> r.setDetailProfileId(matchProfile.getId()));
      addedRelations.stream()
          .skip(1)
          .findFirst()
          .ifPresent(
              r -> {
                r.setMasterProfileId(matchProfile.getId());
                r.setDetailProfileId(actionProfileToUpdateSrs.getId());
              });
      return jobProfilePost;
    }
  }
}
