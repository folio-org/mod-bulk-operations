package org.folio.bulkops.processor;

import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;

import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.ErrorService;
import org.springframework.beans.factory.annotation.Autowired;

@Log4j2
public abstract class FolioAbstractUpdateProcessor<T extends BulkOperationsEntity> implements FolioUpdateProcessor<T> {
  private ErrorService errorService;

  @Autowired
  private void setErrorService(ErrorService errorService) {
    this.errorService = errorService;
  }

  @Override
  public void updateAssociatedRecords(T t, BulkOperation operation, boolean notChanged) {
    if (notChanged) {
      errorService.saveError(operation.getId(), t.getIdentifier(operation.getIdentifierType()), MSG_NO_CHANGE_REQUIRED, ErrorType.WARNING);
    }
  }
}
