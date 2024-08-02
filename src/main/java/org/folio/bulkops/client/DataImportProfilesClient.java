package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.JobProfileCollection;
import org.folio.bulkops.domain.bean.JobProfilePost;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "data-import-profiles", configuration = FeignClientConfiguration.class)
public interface DataImportProfilesClient {

  @GetMapping(value = "/jobProfiles", produces = MediaType.APPLICATION_JSON_VALUE)
  JobProfileCollection getJobProfiles();

  @PostMapping(value = "/jobProfiles")
  void createJobProfile(@RequestBody JobProfilePost jobProfilePost);
}
