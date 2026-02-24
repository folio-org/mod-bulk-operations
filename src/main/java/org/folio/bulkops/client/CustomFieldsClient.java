package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.CustomFieldCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "custom-fields", accept = MediaType.APPLICATION_JSON_VALUE)
public interface CustomFieldsClient {

  @GetExchange
  CustomFieldCollection getByQuery(
      @RequestHeader(value = "x-okapi-module-id") String moduleId,
      @RequestParam("query") String query);
}
