package org.folio.bulkops.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.ActionProfile;
import org.folio.bulkops.domain.bean.JobProfile;
import org.folio.bulkops.domain.bean.MappingProfile;
import org.folio.bulkops.domain.bean.MatchProfile;
import org.folio.bulkops.domain.bean.ProfileAssociation;
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

  public ActionProfile getActionProfile() throws IOException {
    var objectMapper = new ObjectMapper();
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "action_profile.json")) {
      return objectMapper.readValue(is, ActionProfile.class);
    }
  }

  public MappingProfile getMappingProfile() throws IOException {
    var objectMapper = new ObjectMapper();
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "mapping_profile.json")) {
      return objectMapper.readValue(is, MappingProfile.class);
    }
  }

  public JobProfile getJobProfile() throws IOException {
    var objectMapper = new ObjectMapper();
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "job_profile.json")) {
      return objectMapper.readValue(is, JobProfile.class);
    }
  }

  public ProfileAssociation getProfileAssociation() throws IOException {
    var objectMapper = new ObjectMapper();
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "profile_association.json")) {
      return objectMapper.readValue(is, ProfileAssociation.class);
    }
  }
}