package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignEncoderConfiguration;
import org.folio.bulkops.domain.bean.Publication;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "instance-storage/instances", configuration = FeignEncoderConfiguration.class)
public interface PublicationClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  List<Publication> getByQuery(@RequestParam String query);
}
