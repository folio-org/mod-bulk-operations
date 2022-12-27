package org.folio.bulkops.processor;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DataProcessorFactory {

  private DataProcessor<User> userDataProcessor;
  private DataProcessor<Item> itemDataProcessor;
  private DataProcessor<HoldingsRecord> holdingsDataProcessor;

  @Autowired
  private List<DataProcessor<? extends BulkOperationsEntity>> services;
  private Map<Class<? extends BulkOperationsEntity>, DataProcessor<? extends BulkOperationsEntity>> pool;

  @PostConstruct
  private void initPostConstruct() {
    for (DataProcessor<? extends BulkOperationsEntity> service : services) {
      pool.put(service.getProcessedType(), service);
    }
  }

  public <T> DataProcessor<T> getProcessorFromFactory(Class<T> clazz) {
    return (DataProcessor<T>) pool.get(clazz);
  }
}
