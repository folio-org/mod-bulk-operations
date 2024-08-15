package org.folio.bulkops.builder;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;

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
  void shouldGetJobProfile() {
    var jobProfile = dataImportProfilesBuilder.getJobProfile();
    assertNotNull(jobProfile);
  }


  @Test
  @SneakyThrows
  void shouldGetProfileAssociation() {
    var profileAssociation = dataImportProfilesBuilder.getProfileAssociation();
    assertNotNull(profileAssociation);
  }
}
