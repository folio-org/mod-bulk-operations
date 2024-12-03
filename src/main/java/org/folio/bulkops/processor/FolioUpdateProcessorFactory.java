package org.folio.bulkops.processor;

import java.util.List;
import java.util.Map;

import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.User;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class FolioUpdateProcessorFactory {

  private FolioUpdateProcessor<User> userUpdateProcessor;
  private FolioUpdateProcessor<ExtendedItem> itemUpdateProcessor;
  private FolioUpdateProcessor<ExtendedHoldingsRecord> holdingsUpdateProcessor;
  private FolioUpdateProcessor<ExtendedInstance> instanceUpdateProcessor;

  private List<FolioUpdateProcessor<? extends BulkOperationsEntity>> processors;
  private Map<Class<? extends BulkOperationsEntity>, FolioUpdateProcessor<? extends BulkOperationsEntity>> pool;

  @PostConstruct
  private void initPostConstruct() {
    for (FolioUpdateProcessor<? extends BulkOperationsEntity> processor : processors) {
      pool.put(processor.getUpdatedType(), processor);
    }
  }

  public <T extends BulkOperationsEntity> FolioUpdateProcessor<T> getProcessorFromFactory(Class<? extends BulkOperationsEntity> clazz) {
    return (FolioUpdateProcessor<T>) pool.get(clazz);
  }
}
