package org.folio.bulkops.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.domain.bean.ItemCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory/items")
public interface ItemClient {
  @PatchMapping(value = "/{itemId}")
  void patchItem(@RequestBody ObjectNode patch, @PathVariable String itemId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ItemCollection getByQuery(@RequestParam("query") String query, @RequestParam("limit") int limit);
}
