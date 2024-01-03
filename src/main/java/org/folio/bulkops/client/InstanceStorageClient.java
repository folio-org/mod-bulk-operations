package org.folio.bulkops.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.bulkops.domain.bean.Instance;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "instance-storage/instances")
public interface InstanceStorageClient {
  @PutMapping(value = "/{instanceId}")
  void updateInstance(@RequestBody Instance instance, @PathVariable String instanceId);
}
