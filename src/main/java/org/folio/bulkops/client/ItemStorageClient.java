package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.ItemCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "item-storage/items", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ItemStorageClient {

  @GetExchange
  ItemCollection getByQuery(@RequestParam("query") String query, @RequestParam("limit") int limit);
}
