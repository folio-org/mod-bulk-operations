package org.folio.bulkops.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.util.Constants.ERROR_COMMITTING_FILE_NAME_PREFIX;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.processor.marc.MarcFlowCommitProcessor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BulkOperationServiceHelper {
  private final BulkOperationRepository bulkOperationRepository;
  private final ErrorService errorService;
  private final MarcFlowCommitProcessor marcFlowCommitProcessor;
  private final LogFilesService logFilesService;

  public void completeBulkOperation(BulkOperation bulkOperation) {
    bulkOperation.setTotalNumOfRecords(bulkOperation.getMatchedNumOfRecords());
    bulkOperation.setLinkToCommittedRecordsErrorsCsvFile(errorService.uploadErrorsToStorage(bulkOperation.getId(), ERROR_COMMITTING_FILE_NAME_PREFIX, null));
    if (nonNull(bulkOperation.getLinkToCommittedRecordsErrorsCsvFile())) {
      bulkOperation.setCommittedNumOfErrors(errorService.getCommittedNumOfErrors(bulkOperation.getId()));
      bulkOperation.setCommittedNumOfWarnings(errorService.getCommittedNumOfWarnings(bulkOperation.getId()));
    }
    bulkOperation.setStatus(isEmpty(bulkOperation.getLinkToCommittedRecordsErrorsCsvFile()) ? COMPLETED : COMPLETED_WITH_ERRORS);
    if (INSTANCE_MARC.equals(bulkOperation.getEntityType())) {
      marcFlowCommitProcessor.processCommit(bulkOperation);
    }
    bulkOperation.setProcessedNumOfRecords(bulkOperation.getCommittedNumOfRecords());
    bulkOperation.setEndTime(LocalDateTime.now());
    bulkOperationRepository.save(bulkOperation);
  }

  public void failCommit(BulkOperation bulkOperation, Exception e) {
    logFilesService.removeCommittedFiles(bulkOperation);
    bulkOperation.setErrorMessage(e.getMessage());
    var linkToCommittingErrorsFile = errorService.uploadErrorsToStorage(bulkOperation.getId(), ERROR_COMMITTING_FILE_NAME_PREFIX, bulkOperation.getErrorMessage());
    bulkOperation.setLinkToCommittedRecordsErrorsCsvFile(linkToCommittingErrorsFile);
    bulkOperation.setStatus(FAILED);
    bulkOperation.setEndTime(LocalDateTime.now());
    bulkOperationRepository.save(bulkOperation);
  }
}
