package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "inventory/items", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ItemClient {
  @PutExchange(value = "/{itemId}")
  void updateItem(@RequestBody Item item, @PathVariable String itemId);

  @GetExchange
  ItemCollection getByQuery(@RequestParam("query") String query, @RequestParam("limit") int limit);
}
