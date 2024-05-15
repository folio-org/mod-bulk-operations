package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.MSG_ERROR_TEMPLATE_OPTIMISTIC_LOCKING;
import static org.folio.bulkops.util.Utils.resolveEntityClass;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.OptimisticLockingException;
import org.folio.bulkops.processor.UpdateProcessorFactory;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordUpdateService {
  private final UpdateProcessorFactory updateProcessorFactory;
  private final BulkOperationExecutionContentRepository executionContentRepository;

  public BulkOperationsEntity updateEntity(BulkOperationsEntity original, BulkOperationsEntity modified, BulkOperation operation) {
    var isEqual = original.hashCode() == modified.hashCode() && original.equals(modified);
    var updater = updateProcessorFactory.getProcessorFromFactory(resolveEntityClass(operation.getEntityType()));
    if (!isEqual) {
      try {
        updater.updateRecord(modified);
      } catch (FeignException e) {
        if (e.status() == 409 && e.getMessage().contains("optimistic locking")) {
          throw new OptimisticLockingException(String.format(MSG_ERROR_TEMPLATE_OPTIMISTIC_LOCKING, original._version(), modified._version()));
        }
        throw e;
      }
      executionContentRepository.save(BulkOperationExecutionContent.builder()
        .bulkOperationId(operation.getId())
        .identifier(modified.getIdentifier(operation.getIdentifierType()))
        .state(StateType.PROCESSED)
        .build());
      operation.setCommittedNumOfRecords(operation.getCommittedNumOfRecords() + 1);
    }
    updater.updateAssociatedRecords(modified, operation, isEqual);
    return isEqual ? original : modified;
  }
}
