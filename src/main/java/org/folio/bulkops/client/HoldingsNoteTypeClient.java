package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsNoteTypeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "holdings-note-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface HoldingsNoteTypeClient {
  @GetExchange(value = "/{id}")
  HoldingsNoteType getNoteTypeById(@PathVariable String id);

  @GetExchange
  HoldingsNoteTypeCollection getNoteTypesByQuery(
      @RequestParam String query, @RequestParam("limit") int limit);

  @GetExchange
  HoldingsNoteTypeCollection getNoteTypes(@RequestParam("limit") int limit);
}
