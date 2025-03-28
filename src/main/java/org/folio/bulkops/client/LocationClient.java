package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.ItemLocationCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "locations", configuration = FeignClientConfiguration.class)
public interface LocationClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ItemLocationCollection getByQuery(@RequestParam String query);

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  ItemLocation getLocationById(@PathVariable String id);
}
