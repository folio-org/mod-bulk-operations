package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.config.FeignEncoderConfiguration;
import org.folio.bulkops.domain.bean.Item;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "inventory/items")
public interface ItemClient {
  @PutMapping(value = "/{itemId}")
  void updateItem(@RequestBody Item item, @PathVariable String itemId);
}
