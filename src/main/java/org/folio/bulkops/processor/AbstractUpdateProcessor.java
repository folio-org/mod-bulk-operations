package org.folio.bulkops.processor;

import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;

import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.ErrorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public abstract class AbstractUpdateProcessor<T extends BulkOperationsEntity> implements UpdateProcessor<T> {
  @Autowired
  private ErrorService errorService;

  @Override
  public void updateAssociatedRecords(T t, BulkOperation operation, boolean notChanged) {
    if (notChanged) {
      errorService.saveError(operation.getId(), t.getIdentifier(operation.getIdentifierType()), MSG_NO_CHANGE_REQUIRED);
    }
  }
}
