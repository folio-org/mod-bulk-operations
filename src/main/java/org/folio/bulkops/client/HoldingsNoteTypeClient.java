package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsNoteTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "holdings-note-types", configuration = FeignClientConfiguration.class)
public interface HoldingsNoteTypeClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsNoteType getNoteTypeById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsNoteTypeCollection getNoteTypesByQuery(@RequestParam String query,
                                                 @RequestParam("limit") int limit);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsNoteTypeCollection getNoteTypes(@RequestParam("limit") int limit);
}
