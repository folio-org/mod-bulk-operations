package org.folio.bulkops.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.ActionProfile;
import org.folio.bulkops.domain.bean.ActionProfilePost;
import org.folio.bulkops.domain.bean.JobProfilePost;
import org.folio.bulkops.domain.bean.MappingProfile;
import org.folio.bulkops.domain.bean.MatchProfile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Log4j2
public class DataImportProfilesBuilder {

  private static final String DATA_IMPORT_PROFILES_PATH = "/import/profiles/";

  public MatchProfile getMatchProfile() throws IOException {
    var objectMapper = new ObjectMapper();
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "match_profile.json")) {
      return objectMapper.readValue(is, MatchProfile.class);
    }
  }

  public ActionProfilePost getActionProfilePostToUpdateInstance(MappingProfile mappingProfileToUpdateInstance) throws IOException {
    var objectMapper = new ObjectMapper();
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "action_update_instance_profile_post.json")) {
      var actionProfile = objectMapper.readValue(is, ActionProfilePost.class);
      actionProfile.getAddedRelations().get(0).setDetailProfileId(mappingProfileToUpdateInstance.getId());
      return actionProfile;
    }
  }

  public ActionProfilePost getActionProfilePostToUpdateSrs(MappingProfile mappingProfileToUpdateSrs) throws IOException {
    var objectMapper = new ObjectMapper();
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "action_update_srs_profile_post.json")) {
      var actionProfile = objectMapper.readValue(is, ActionProfilePost.class);
      actionProfile.getAddedRelations().get(0).setDetailProfileId(mappingProfileToUpdateSrs.getId());
      return actionProfile;
    }
  }

  public MappingProfile getMappingProfileToUpdateInstance() throws IOException {
    var objectMapper = new ObjectMapper();
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "mapping_profile_update_instance.json")) {
      return objectMapper.readValue(is, MappingProfile.class);
    }
  }

  public MappingProfile getMappingProfileToUpdateSrs() throws IOException {
    var objectMapper = new ObjectMapper();
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "mapping_profile_update_srs.json")) {
      return objectMapper.readValue(is, MappingProfile.class);
    }
  }

  public JobProfilePost getJobProfilePost(MatchProfile matchProfile, ActionProfile actionProfileToUpdateInstance, ActionProfile actionProfileToUpdateSrs) throws IOException {
    var objectMapper = new ObjectMapper();
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "job_profile_post.json")) {
      var jobProfilePost = objectMapper.readValue(is, JobProfilePost.class);
      jobProfilePost.getAddedRelations().get(0).setDetailProfileId(matchProfile.getId());
      jobProfilePost.getAddedRelations().get(1).setMasterProfileId(matchProfile.getId());
      jobProfilePost.getAddedRelations().get(1).setDetailProfileId(actionProfileToUpdateSrs.getId());
      jobProfilePost.getAddedRelations().get(2).setMasterProfileId(matchProfile.getId());
      jobProfilePost.getAddedRelations().get(2).setDetailProfileId(actionProfileToUpdateInstance.getId());
      return jobProfilePost;
    }
  }
}
