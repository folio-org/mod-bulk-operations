package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.config.FeignEncoderConfiguration;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.configs.FeignEncoderConfiguration;
import org.folio.bulkops.domain.dto.Item;
import org.folio.bulkops.domain.dto.ItemCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory/items")
public interface ItemClient {
  @PutMapping(value = "/{itemId}")
  void updateItem(@RequestBody Item item, @PathVariable String itemId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ItemCollection getItemByQuery(@RequestParam("query") String query, @RequestParam long offset, @RequestParam long limit);

}
