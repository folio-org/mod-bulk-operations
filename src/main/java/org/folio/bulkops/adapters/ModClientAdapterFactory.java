package org.folio.bulkops.adapters;

import java.util.List;
import java.util.Map;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ModClientAdapterFactory {

  private UnifiedTableHeaderBuilder<User> userUnifiedTableHeaderBuilderAdapter;
  private UnifiedTableHeaderBuilder<Item> itemUnifiedTableHeaderBuilderAdapter;
  private UnifiedTableHeaderBuilder<HoldingsRecord> holdingsUnifiedTableHeaderBuilderAdapter;

  @Autowired
  private List<UnifiedTableHeaderBuilder<? extends BulkOperationsEntity>> services;
  private Map<Class<? extends BulkOperationsEntity>, UnifiedTableHeaderBuilder<? extends BulkOperationsEntity>> pool;

  @PostConstruct
  private void initPostConstruct() {
    for (UnifiedTableHeaderBuilder<? extends BulkOperationsEntity> service : services) {
      pool.put(service.getProcessedType(), service);
    }
  }

  public <T extends BulkOperationsEntity> UnifiedTableHeaderBuilder<T> getModClientAdapter(Class<? extends BulkOperationsEntity> clazz) {
    return (UnifiedTableHeaderBuilder<T>) pool.get(clazz);
  }
}
