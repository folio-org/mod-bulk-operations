package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.ItemCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "item-storage/items")
public interface ItemStorageClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ItemCollection getByQuery(@RequestParam("query") String query, @RequestParam("limit") int limit);

}
