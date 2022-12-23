package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.dto.ItemLocation;

import org.folio.bulkops.domain.dto.ItemLocationCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;


@FeignClient(name = "locations", configuration = FeignClientConfiguration.class)
public interface LocationClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getLocation(@PathVariable String id);

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  ItemLocation getLocationById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ItemLocationCollection getLocationByQuery(@RequestParam String query);
}
