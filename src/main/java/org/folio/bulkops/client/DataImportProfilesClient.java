package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.domain.bean.ActionProfile;
import org.folio.bulkops.domain.bean.ActionProfilePost;
import org.folio.bulkops.domain.bean.JobProfile;
import org.folio.bulkops.domain.bean.JobProfilePost;
import org.folio.bulkops.domain.bean.MappingProfile;
import org.folio.bulkops.domain.bean.MappingProfilePost;
import org.folio.bulkops.domain.bean.MatchProfile;
import org.folio.bulkops.domain.bean.MatchProfilePost;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "data-import-profiles")
public interface DataImportProfilesClient {

  @PostExchange(value = "/jobProfiles")
  JobProfile createJobProfile(@RequestBody JobProfilePost jobProfilePost);

  @PostExchange(value = "/matchProfiles")
  MatchProfile createMatchProfile(@RequestBody MatchProfilePost matchProfilePost);

  @PostExchange(value = "/actionProfiles")
  ActionProfile createActionProfile(@RequestBody ActionProfilePost actionProfilePost);

  @PostExchange(value = "/mappingProfiles")
  MappingProfile createMappingProfile(@RequestBody MappingProfilePost mappingProfilePost);
}
