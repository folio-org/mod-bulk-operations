package org.folio.bulkops.processor;

import java.util.List;
import java.util.Map;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UpdateProcessorFactory {

  private UpdateProcessor<User> userUpdateProcessor;
  private UpdateProcessor<ExtendedItem> itemUpdateProcessor;
  private UpdateProcessor<ExtendedHoldingsRecord> holdingsUpdateProcessor;
  private UpdateProcessor<ExtendedInstance> instanceUpdateProcessor;

  @Autowired
  private List<UpdateProcessor<? extends BulkOperationsEntity>> processors;
  private Map<Class<? extends BulkOperationsEntity>, UpdateProcessor<? extends BulkOperationsEntity>> pool;

  @PostConstruct
  private void initPostConstruct() {
    for (UpdateProcessor<? extends BulkOperationsEntity> processor : processors) {
      pool.put(processor.getUpdatedType(), processor);
    }
  }

  public <T extends BulkOperationsEntity> UpdateProcessor<T> getProcessorFromFactory(Class<? extends BulkOperationsEntity> clazz) {
    return (UpdateProcessor<T>) pool.get(clazz);
  }
}
