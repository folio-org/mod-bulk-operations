package org.folio.bulkops.client;

import org.folio.bulkops.domain.dto.InstanceNoteType;
import org.folio.bulkops.domain.dto.InstanceNoteTypeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "instance-note-types", accept = MediaType.APPLICATION_JSON_VALUE)
public interface InstanceNoteTypesClient {
  @GetExchange(value = "/{id}")
  InstanceNoteType getNoteTypeById(@PathVariable String id);

  @GetExchange
  InstanceNoteTypeCollection getNoteTypesByQuery(
      @RequestParam String query, @RequestParam("limit") int limit);

  @GetExchange
  InstanceNoteTypeCollection getInstanceNoteTypes(@RequestParam("limit") int limit);
}
