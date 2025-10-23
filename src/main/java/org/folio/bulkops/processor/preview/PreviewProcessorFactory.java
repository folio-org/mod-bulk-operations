package org.folio.bulkops.processor.preview;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PreviewProcessorFactory {

  private PreviewRowProcessor<User> userPreviewProcessor;
  private PreviewRowProcessor<Item> itemPreviewProcessor;
  private PreviewRowProcessor<HoldingsRecord> holdingsPreviewProcessor;
  private PreviewRowProcessor<Instance> instancePreviewProcessor;

  private List<PreviewRowProcessor<? extends BulkOperationsEntity>> processors;
  private Map<Class<? extends BulkOperationsEntity>, PreviewRowProcessor<?
          extends BulkOperationsEntity>> pool;

  @PostConstruct
  private void initPostConstruct() {
    for (PreviewRowProcessor<? extends BulkOperationsEntity> service : processors) {
      pool.put(service.getProcessedType(), service);
    }
  }

  public <T extends BulkOperationsEntity> PreviewRowProcessor<T> getProcessorFromFactory(
          Class<? extends BulkOperationsEntity> clazz) {
    return (PreviewRowProcessor<T>) pool.get(clazz);
  }
}
