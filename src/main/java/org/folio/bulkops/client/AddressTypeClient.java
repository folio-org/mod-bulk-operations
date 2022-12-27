package org.folio.bulkops.client;

import org.folio.bulkops.configs.FeignClientConfiguration;
import org.folio.bulkops.domain.bean.AddressType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "addresstypes", configuration = FeignClientConfiguration.class)
public interface AddressTypeClient {

  @GetMapping(value = "/{typeId}", produces = MediaType.APPLICATION_JSON_VALUE)
  AddressType getAddressTypeById(@PathVariable String typeId);
}
