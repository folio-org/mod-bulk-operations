package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

@FeignClient(name = "inventory/items", configuration = FeignClientConfiguration.class)
public interface ItemClient {
  @PutMapping(value = "/{itemId}")
  void updateItem(@RequestBody Item item, @PathVariable String itemId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ItemCollection getByQuery(@RequestParam("query") String query, @RequestParam("limit") int limit);

}
