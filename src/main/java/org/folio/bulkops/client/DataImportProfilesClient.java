package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.ActionProfile;
import org.folio.bulkops.domain.bean.ActionProfilePost;
import org.folio.bulkops.domain.bean.JobProfile;
import org.folio.bulkops.domain.bean.JobProfileCollection;
import org.folio.bulkops.domain.bean.JobProfilePost;
import org.folio.bulkops.domain.bean.MappingProfile;
import org.folio.bulkops.domain.bean.MappingProfilePost;
import org.folio.bulkops.domain.bean.MatchProfile;
import org.folio.bulkops.domain.bean.MatchProfilePost;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "data-import-profiles", configuration = FeignClientConfiguration.class)
public interface DataImportProfilesClient {

  @GetMapping(value = "/jobProfiles", produces = MediaType.APPLICATION_JSON_VALUE)
  JobProfileCollection getJobProfiles();

  @PostMapping(value = "/jobProfiles")
  JobProfile createJobProfile(@RequestBody JobProfilePost jobProfilePost);

  @PostMapping(value = "/matchProfiles")
  MatchProfile createMatchProfile(@RequestBody MatchProfilePost matchProfilePost);

  @PostMapping(value = "/actionProfiles")
  ActionProfile createActionProfile(@RequestBody ActionProfilePost actionProfilePost);

  @PostMapping(value = "/mappingProfiles")
  MappingProfile createMappingProfile(@RequestBody MappingProfilePost mappingProfilePost);
}
