package org.folio.bulkops.processor.preview;

import java.util.ArrayList;
import java.util.Arrays;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.util.CustomMappingStrategy;

@Log4j2
public abstract class AbstractPreviewProcessor<T extends BulkOperationsEntity>
    implements PreviewRowProcessor<T> {
  @Override
  public Row transformToRow(T entity) {
    CustomMappingStrategy<T> strategy = new CustomMappingStrategy<>();
    strategy.setType(getProcessedType());
    var res = new Row();

    try {
      strategy.generateHeader(entity);
      res.setRow(new ArrayList<>(Arrays.asList(strategy.transmuteBean(entity))));
    } catch (Exception e) {
      log.error("Failed to create row for preview", e);
    }

    return res;
  }
}
