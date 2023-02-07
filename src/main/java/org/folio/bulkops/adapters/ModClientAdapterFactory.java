package org.folio.bulkops.adapters;

import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ModClientAdapterFactory {

  private ModClient<User> userModClientAdapter;
  private ModClient<Item> itemModClientAdapter;
  private ModClient<HoldingsRecord> holdingsModClientAdapter;

  @Autowired
  private List<ModClient<? extends BulkOperationsEntity>> services;
  private Map<Class<? extends BulkOperationsEntity>, ModClient<? extends BulkOperationsEntity>> pool;

  @PostConstruct
  private void initPostConstruct() {
    for (ModClient<? extends BulkOperationsEntity> service : services) {
      pool.put(service.getProcessedType(), service);
    }
  }

  public <T extends BulkOperationsEntity> ModClient<T> getModClientAdapter(Class<? extends BulkOperationsEntity> clazz) {
    return (ModClient<T>) pool.get(clazz);
  }
}
