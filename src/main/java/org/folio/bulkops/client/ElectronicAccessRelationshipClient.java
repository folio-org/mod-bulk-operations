package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.ElectronicAccessRelationship;
import org.folio.bulkops.domain.bean.ElectronicAccessRelationshipCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "electronic-access-relationships", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ElectronicAccessRelationshipClient {
  @GetExchange(value = "/{id}")
  ElectronicAccessRelationship getById(@PathVariable String id);

  @GetExchange
  ElectronicAccessRelationshipCollection getByQuery(@RequestParam String query);
}
