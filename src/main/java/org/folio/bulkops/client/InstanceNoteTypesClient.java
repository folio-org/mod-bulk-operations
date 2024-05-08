package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.dto.InstanceNoteType;
import org.folio.bulkops.domain.dto.InstanceNoteTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "instance-note-types", configuration = FeignClientConfiguration.class)
public interface InstanceNoteTypesClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceNoteType getNoteTypeById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceNoteTypeCollection getNoteTypesByQuery(@RequestParam String query, @RequestParam("limit") int limit);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceNoteTypeCollection getInstanceNoteTypes(@RequestParam("limit") int limit);
}
