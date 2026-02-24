package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.bean.NoteTypeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "item-note-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ItemNoteTypeClient {
  @GetExchange(value = "/{id}")
  NoteType getNoteTypeById(@PathVariable String id);

  @GetExchange
  NoteTypeCollection getNoteTypesByQuery(
      @RequestParam String query, @RequestParam("limit") int limit);

  @GetExchange
  NoteTypeCollection getNoteTypes(@RequestParam("limit") int limit);
}
