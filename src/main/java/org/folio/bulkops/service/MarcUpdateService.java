package org.folio.bulkops.service;

import static org.folio.bulkops.domain.dto.IdentifierType.HRID;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_MARC_CHANGES;
import static org.folio.bulkops.util.Constants.CHANGED_MARC_PATH_TEMPLATE;
import static org.folio.bulkops.util.Constants.ERROR_COMMITTING_FILE_NAME_PREFIX;
import static org.folio.bulkops.util.Constants.MSG_NO_MARC_CHANGE_REQUIRED;
import static org.folio.bulkops.util.MarcHelper.fetchInstanceUuidOrElseHrid;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.StatusType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.processor.marc.MarcInstanceUpdateProcessor;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.MarcDateHelper;
import org.marc4j.MarcStreamReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

@Service
@Log4j2
@RequiredArgsConstructor
public class MarcUpdateService {
  private final BulkOperationExecutionRepository executionRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final MarcInstanceUpdateProcessor updateProcessor;
  private final ErrorService errorService;
  private final BulkOperationRepository bulkOperationRepository;
  private final BulkOperationServiceHelper bulkOperationServiceHelper;

  @Transactional
  public void commitForInstanceMarc(BulkOperation bulkOperation, Set<String> failedHrids) {
    if (StringUtils.isNotEmpty(bulkOperation.getLinkToModifiedRecordsMarcFile())) {
      bulkOperation.setStatus(APPLY_MARC_CHANGES);
      bulkOperationRepository.save(bulkOperation);

      var execution = executionRepository.save(BulkOperationExecution.builder()
        .bulkOperationId(bulkOperation.getId())
        .startTime(LocalDateTime.now())
        .processedRecords(0)
        .status(StatusType.ACTIVE)
        .build());

      try {
        bulkOperation.setLinkToCommittedRecordsMarcFile(prepareCommittedFile(bulkOperation, failedHrids));
        updateProcessor.updateMarcRecords(bulkOperation);
        execution = execution
          .withStatus(StatusType.COMPLETED)
          .withEndTime(LocalDateTime.now());
      } catch (IOException e) {
        log.error("Error while updating marc file", e);
        execution = execution
          .withStatus(StatusType.FAILED)
          .withEndTime(LocalDateTime.now());
        bulkOperationServiceHelper.failCommit(bulkOperation, e);
      }
      executionRepository.save(execution);
    } else {
      bulkOperation.setCommittedNumOfErrors(errorService.getCommittedNumOfErrors(bulkOperation.getId()));
      bulkOperation.setCommittedNumOfWarnings(errorService.getCommittedNumOfWarnings(bulkOperation.getId()));
    }
  }

  private String prepareCommittedFile(BulkOperation bulkOperation, Set<String> failedHrids) throws IOException {
    var triggeringFileName = FilenameUtils.getBaseName(bulkOperation.getLinkToTriggeringCsvFile());
    var resultMarcFileName = String.format(CHANGED_MARC_PATH_TEMPLATE, bulkOperation.getId(), LocalDate.now(), triggeringFileName);

    try (var writerForResultMarcFile = remoteFileSystemClient.marcWriter(resultMarcFileName);
         var matchedRecordsMarcFileStream = remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsMarcFile());
         var modifiedRecordsMarcFileStream = remoteFileSystemClient.get(bulkOperation.getLinkToModifiedRecordsMarcFile())) {
      var matchedRecordsReader = new MarcStreamReader(matchedRecordsMarcFileStream);
      var modifiedRecordsReader = new MarcStreamReader(modifiedRecordsMarcFileStream);

      var currentDate = new Date();
      while (matchedRecordsReader.hasNext() && modifiedRecordsReader.hasNext()) {
        var originalRecord = matchedRecordsReader.next();
        var modifiedRecord = modifiedRecordsReader.next();
        if (!failedHrids.contains(originalRecord.getControlNumber())) {
          if (originalRecord.toString().equals(modifiedRecord.toString())) {
            var identifier = HRID.equals(bulkOperation.getIdentifierType()) ?
              originalRecord.getControlNumber() :
              fetchInstanceUuidOrElseHrid(originalRecord);
            errorService.saveError(bulkOperation.getId(), identifier, MSG_NO_MARC_CHANGE_REQUIRED, ErrorType.WARNING);
          } else {
            MarcDateHelper.updateDateTimeControlField(modifiedRecord, currentDate);
            writerForResultMarcFile.writeRecord(modifiedRecord);
          }
        }
      }
      return resultMarcFileName;
    }
  }

  public void prepareProgress(BulkOperation bulkOperation) {
    var processedNumOfRecords = errorService.getCommittedNumOfErrors(bulkOperation.getId());
    bulkOperation.setProcessedNumOfRecords(processedNumOfRecords);
    bulkOperation.setCommittedNumOfErrors(processedNumOfRecords);
    bulkOperationRepository.save(bulkOperation);
  }
}
