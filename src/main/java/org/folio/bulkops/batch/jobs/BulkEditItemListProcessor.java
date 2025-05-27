package org.folio.bulkops.batch.jobs;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.ExtendedItemCollection;
import org.folio.bulkops.processor.EntityExtractor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemListProcessor implements ItemProcessor<ExtendedItemCollection, List<ExtendedItem>>, EntityExtractor {

  @Override
  public List<ExtendedItem> process(ExtendedItemCollection extendedItemCollection) {
    return extendedItemCollection.getExtendedItems();
  }
}
