package org.folio.bulkops.service;

import static java.util.Objects.nonNull;
import static org.folio.bulkops.domain.dto.IdentifierType.HRID;
import static org.folio.bulkops.domain.dto.OperationStatusType.APPLY_MARC_CHANGES;
import static org.folio.bulkops.domain.dto.OperationStatusType.FAILED;
import static org.folio.bulkops.util.Constants.MARC;
import static org.folio.bulkops.util.Constants.MSG_NO_MARC_CHANGE_REQUIRED;
import static org.folio.bulkops.util.MarcHelper.fetchInstanceUuidOrElseHrid;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.ExtendedInstance;
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

@Service
@Log4j2
@RequiredArgsConstructor
public class MarcUpdateService {
  public static final String CHANGED_MARC_PATH_TEMPLATE = "%s/%s-Changed-Records-MARC-%s.mrc";
  public static final String MSG_BULK_EDIT_SUPPORTED_FOR_MARC_ONLY = "Instance with source %s is not supported by MARC records bulk edit and cannot be updated.";
  public static final String CHANGED_MARC_CSV_PATH_TEMPLATE = "%s/%s-Changed-Records-MARC-CSV-%s.csv";

  private final BulkOperationExecutionRepository executionRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final MarcInstanceUpdateProcessor updateProcessor;
  private final ErrorService errorService;
  private final BulkOperationRepository bulkOperationRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public void commitForInstanceMarc(BulkOperation bulkOperation) {
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
        bulkOperation.setLinkToCommittedRecordsMarcFile(prepareCommittedFile(bulkOperation));
        updateProcessor.updateMarcRecords(bulkOperation);
        var path = bulkOperation.getLinkToCommittedRecordsMarcFile();
        if (nonNull(path)) {
          remoteFileSystemClient.remove(path);
          bulkOperation.setLinkToCommittedRecordsMarcFile(null);
        }
        bulkOperationRepository.save(bulkOperation);
        execution = execution
          .withStatus(StatusType.COMPLETED)
          .withEndTime(LocalDateTime.now());
      } catch (Exception e) {
        log.error("Error while updating marc file", e);
        execution = execution
          .withStatus(StatusType.FAILED)
          .withEndTime(LocalDateTime.now());
        bulkOperation.setStatus(FAILED);
        bulkOperation.setEndTime(LocalDateTime.now());
        bulkOperation.setErrorMessage(e.getMessage());
        bulkOperationRepository.save(bulkOperation);
      }
      executionRepository.save(execution);
    } else {
      bulkOperation.setCommittedNumOfErrors(errorService.getCommittedNumOfErrors(bulkOperation.getId()));
      bulkOperation.setCommittedNumOfWarnings(errorService.getCommittedNumOfWarnings(bulkOperation.getId()));
    }
  }

  private String prepareCommittedFile(BulkOperation bulkOperation) throws IOException {
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
      return resultMarcFileName;
    }
  }

  public void saveErrorsForFolioInstances(BulkOperation bulkOperation) {
    var numOfFolioInstances = 0;
    saveProgress(bulkOperation, numOfFolioInstances);
    try (var readerForMatchedJsonFile = remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
      var iterator = objectMapper.readValues(new JsonFactory().createParser(readerForMatchedJsonFile), ExtendedInstance.class);
      while (iterator.hasNext()) {
        var instance = iterator.next().getEntity();
        if (!MARC.equals(instance.getSource())) {
          var identifier = HRID.equals(bulkOperation.getIdentifierType()) ?
            instance.getHrid() :
            instance.getId();
          errorService.saveError(bulkOperation.getId(), identifier, MSG_BULK_EDIT_SUPPORTED_FOR_MARC_ONLY.formatted(instance.getSource()), ErrorType.ERROR);
          if (++numOfFolioInstances % 100 == 0) {
            saveProgress(bulkOperation, numOfFolioInstances);
          }
        }
      }
      saveProgress(bulkOperation, numOfFolioInstances);
    } catch (Exception e) {
      log.error("Failed to save errors for folio instances", e);
    }
  }

  private void saveProgress(BulkOperation bulkOperation, int numOfFolioInstances) {
    bulkOperation.setProcessedNumOfRecords(numOfFolioInstances);
    bulkOperation.setCommittedNumOfErrors(numOfFolioInstances);
    bulkOperationRepository.save(bulkOperation);
  }
}
