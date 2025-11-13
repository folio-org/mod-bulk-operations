package org.folio.bulkops.service;

import static java.lang.String.format;
import static org.folio.bulkops.util.Utils.resolveExtendedEntityClass;

import feign.FeignException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.StateType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecutionContent;
import org.folio.bulkops.exception.OptimisticLockingException;
import org.folio.bulkops.processor.folio.FolioUpdateProcessorFactory;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.util.EntityPathResolver;
import org.folio.bulkops.util.Utils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordUpdateService {
  private final FolioUpdateProcessorFactory updateProcessorFactory;
  private final BulkOperationExecutionContentRepository executionContentRepository;
  private final EntityPathResolver entityPathResolver;

  public BulkOperationsEntity updateEntity(BulkOperationsEntity original,
                                           BulkOperationsEntity modified, BulkOperation operation) {
    var entity = modified.getRecordBulkOperationEntity();
    if (Objects.nonNull(entity)) {
      entity.setTenant(null);
    }
    var isEqual = original.hashCode() == modified.hashCode() && original.equals(modified);
    var updater = updateProcessorFactory.getProcessorFromFactory(
            resolveExtendedEntityClass(operation.getEntityType()));
    if (!isEqual) {
      try {
        updater.updateRecord(modified);
      } catch (FeignException e) {
        if (e.status() == 409 && e.getMessage().contains("optimistic locking")) {
          var message = Utils.getMessageFromFeignException(e);
          var link = entityPathResolver.resolve(operation.getEntityType(), original);
          throw new OptimisticLockingException(format("%s %s", message, link), message, link);
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
