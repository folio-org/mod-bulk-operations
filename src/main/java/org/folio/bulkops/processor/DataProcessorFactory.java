package org.folio.bulkops.processor;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DataProcessorFactory<T> {

  private DataProcessor<User> userDataProcessor;
  private DataProcessor<Item> itemDataProcessor;
  private DataProcessor<HoldingsRecord> holdingsDataProcessor;

  @Autowired
  private List<DataProcessor<T>> services;
  private Map<Class<T>, DataProcessor<T>> pool;

  @PostConstruct
  private void init() {
    for (DataProcessor<T> service : services) {
      pool.put(service.getProcessedType(), service);
    }
  }

  public DataProcessor<T> getProcessorFromFactory(Class<? extends BulkOperationsEntity> clazz) {
    return pool.get(clazz);
  }
}
