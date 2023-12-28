package org.folio.bulkops.processor;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.InstanceStorageClient;
import org.folio.bulkops.domain.bean.Instance;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InstanceUpdateProcessor implements UpdateProcessor<Instance> {
  private final InstanceStorageClient instanceStorageClient;

  @Override
  public void updateRecord(Instance instance, String identifier, UUID operationId) {
    instanceStorageClient.updateInstance(instance, instance.getId());
  }

  @Override
  public Class<Instance> getUpdatedType() {
    return Instance.class;
  }
}
