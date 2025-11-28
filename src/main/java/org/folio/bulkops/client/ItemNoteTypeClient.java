package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.bean.NoteTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "item-note-types", configuration = FeignClientConfiguration.class)
public interface ItemNoteTypeClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  NoteType getNoteTypeById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  NoteTypeCollection getNoteTypesByQuery(
      @RequestParam String query, @RequestParam("limit") int limit);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  NoteTypeCollection getNoteTypes(@RequestParam("limit") int limit);
}
