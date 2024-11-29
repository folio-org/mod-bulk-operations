package org.folio.bulkops.processor.folio;

import java.util.List;
import java.util.Map;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.processor.FolioDataProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DataProcessorFactory {

  private FolioDataProcessor<User> userDataProcessor;
  private FolioDataProcessor<ExtendedItem> itemDataProcessor;
  private FolioDataProcessor<ExtendedHoldingsRecord> holdingsDataProcessor;
  private FolioDataProcessor<ExtendedInstance> instanceDataProcessor;

  @Autowired
  private List<FolioDataProcessor<? extends BulkOperationsEntity>> services;
  private Map<Class<? extends BulkOperationsEntity>, FolioDataProcessor<? extends BulkOperationsEntity>> pool;

  @PostConstruct
  private void initPostConstruct() {
    for (FolioDataProcessor<? extends BulkOperationsEntity> service : services) {
      pool.put(service.getProcessedType(), service);
    }
  }

  public <T extends BulkOperationsEntity> FolioDataProcessor<T> getProcessorFromFactory(Class<? extends BulkOperationsEntity> clazz) {
    return (FolioDataProcessor<T>) pool.get(clazz);
  }
}
