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

  private ObjectMapper objectMapper = new ObjectMapper();

  public MatchProfile getMatchProfile() throws IOException {
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "match_profile.json")) {
      return objectMapper.readValue(is, MatchProfile.class);
    } catch (IOException e) {
      log.error("Error loading data import match profile : {}", e.getMessage());
      throw e;
    }
  }

  public ActionProfile getActionProfile() throws IOException {
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "action_profile.json")) {
      return objectMapper.readValue(is, ActionProfile.class);
    } catch (IOException e) {
      log.error("Error loading data import action profile : {}", e.getMessage());
      throw e;
    }
  }

  public MappingProfile getMappingProfile() throws IOException {
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "mapping_profile.json")) {
      return objectMapper.readValue(is, MappingProfile.class);
    } catch (IOException e) {
      log.error("Error loading data import mapping profile : {}", e.getMessage());
      throw e;
    }
  }

  public JobProfile getJobProfile() throws IOException {
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "job_profile.json")) {
      return objectMapper.readValue(is, JobProfile.class);
    } catch (IOException e) {
      log.error("Error loading data import job profile : {}", e.getMessage());
      throw e;
    }
  }

  public ProfileAssociation getProfileAssociation() throws IOException {
    try (var is = DataImportProfilesBuilder.class.getResourceAsStream(DATA_IMPORT_PROFILES_PATH + "profile_association.json")) {
      return objectMapper.readValue(is, ProfileAssociation.class);
    } catch (IOException e) {
      log.error("Error loading data import profile association : {}", e.getMessage());
      throw e;
    }
  }
}
