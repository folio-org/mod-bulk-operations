package org.folio.bulkops.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.util.Constants.ERROR_COMMITTING_FILE_NAME_PREFIX;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.processor.marc.MarcFlowCommitProcessor;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class BulkOperationServiceHelper {
  private final BulkOperationRepository bulkOperationRepository;
  private final ErrorService errorService;
  private final MarcFlowCommitProcessor marcFlowCommitProcessor;
  private final LogFilesService logFilesService;

  public void completeBulkOperation(BulkOperation bulkOperation) {
    bulkOperation.setTotalNumOfRecords(bulkOperation.getMatchedNumOfRecords());
    bulkOperation.setLinkToCommittedRecordsErrorsCsvFile(
        errorService.uploadErrorsToStorage(
            bulkOperation.getId(), ERROR_COMMITTING_FILE_NAME_PREFIX, null));
    if (nonNull(bulkOperation.getLinkToCommittedRecordsErrorsCsvFile())) {
      bulkOperation.setCommittedNumOfErrors(
          errorService.getCommittedNumOfErrors(bulkOperation.getId()));
      bulkOperation.setCommittedNumOfWarnings(
          errorService.getCommittedNumOfWarnings(bulkOperation.getId()));
    }
    bulkOperation.setStatus(
        isEmpty(bulkOperation.getLinkToCommittedRecordsErrorsCsvFile())
            ? COMPLETED
            : COMPLETED_WITH_ERRORS);
    if (INSTANCE_MARC.equals(bulkOperation.getEntityType())) {
      marcFlowCommitProcessor.processCommit(bulkOperation);
    }
    bulkOperation.setProcessedNumOfRecords(bulkOperation.getCommittedNumOfRecords());
    bulkOperation.setEndTime(LocalDateTime.now());
    bulkOperationRepository.save(bulkOperation);
  }

  public void failBulkOperation(BulkOperation bulkOperation, Exception e) {
    failBulkOperation(bulkOperation, e.getMessage());
  }

  public void failBulkOperation(BulkOperation bulkOperation, String errorMessage) {
    log.error("Failing bulk operation: {}", errorMessage);
    logFilesService.removeCommittedFiles(bulkOperation);
    bulkOperation.setErrorMessage(errorMessage);
    var linkToCommittingErrorsFile =
        errorService.uploadErrorsToStorage(
            bulkOperation.getId(),
            ERROR_COMMITTING_FILE_NAME_PREFIX,
            bulkOperation.getErrorMessage());
    bulkOperation.setLinkToCommittedRecordsErrorsCsvFile(linkToCommittingErrorsFile);
    bulkOperation.setStatus(FAILED);
    bulkOperation.setEndTime(LocalDateTime.now());
    bulkOperationRepository.save(bulkOperation);
  }
}
