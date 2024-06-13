package org.folio.bulkops.processor;

import java.util.List;
import java.util.Map;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DataProcessorFactory {

  private DataProcessor<User> userDataProcessor;
  private DataProcessor<Item> itemDataProcessor;
  private DataProcessor<ExtendedHoldingsRecord> holdingsDataProcessor;
  private DataProcessor<ExtendedInstance> instanceDataProcessor;

  @Autowired
  private List<DataProcessor<? extends BulkOperationsEntity>> services;
  private Map<Class<? extends BulkOperationsEntity>, DataProcessor<? extends BulkOperationsEntity>> pool;

  @PostConstruct
  private void initPostConstruct() {
    for (DataProcessor<? extends BulkOperationsEntity> service : services) {
      pool.put(service.getProcessedType(), service);
    }
  }

  public <T extends BulkOperationsEntity> DataProcessor<T> getProcessorFromFactory(Class<? extends BulkOperationsEntity> clazz) {
    return (DataProcessor<T>) pool.get(clazz);
  }
}
