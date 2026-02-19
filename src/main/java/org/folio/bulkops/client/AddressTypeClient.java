package org.folio.bulkops.client;

import org.folio.bulkops.domain.bean.AddressType;
import org.folio.bulkops.domain.bean.AddressTypeCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "addresstypes", accept = MediaType.APPLICATION_JSON_VALUE)
public interface AddressTypeClient {

  @GetExchange(value = "/{typeId}")
  AddressType getAddressTypeById(@PathVariable String typeId);

  @GetExchange
  AddressTypeCollection getByQuery(@RequestParam String query);
}
