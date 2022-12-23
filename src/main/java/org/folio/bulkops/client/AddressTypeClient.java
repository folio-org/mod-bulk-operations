package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.dto.AddressType;
import org.folio.bulkops.domain.dto.AddressTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "addresstypes", configuration = FeignClientConfiguration.class)
public interface AddressTypeClient {

  @GetMapping(value = "/{typeId}", produces = MediaType.APPLICATION_JSON_VALUE)
  AddressType getAddressTypeById(@PathVariable String typeId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  AddressTypeCollection getAddressTypeByQuery(@RequestParam String query);

}
