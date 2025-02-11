package org.folio.bulkops.service;

import static com.opencsv.ICSVWriter.DEFAULT_SEPARATOR;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;
import static org.folio.bulkops.service.MarcUpdateService.CHANGED_MARC_CSV_PATH_TEMPLATE;
import static org.folio.bulkops.service.MarcUpdateService.CHANGED_MARC_PATH_TEMPLATE;

import com.opencsv.CSVWriterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.GetParsedRecordsBatchConditions;
import org.folio.bulkops.domain.bean.GetParsedRecordsBatchRequestBody;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.repository.BulkOperationExecutionRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.MarcCsvHelper;
import org.marc4j.MarcJsonReader;
import org.marc4j.marc.Record;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class SrsService {
  private static final int SRS_CHUNK_SIZE = 100;

  private final SrsClient srsClient;
  private final BulkOperationRepository bulkOperationRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final ErrorService errorService;
  private final BulkOperationExecutionRepository executionRepository;
  private final MarcCsvHelper marcCsvHelper;

  public void retrieveMarcInstancesFromSrs(List<String> instanceIds, BulkOperation bulkOperation) {
    log.info("Retrieving {} marc instances from SRS", instanceIds.size());
    var fetchedNumOfRecords = 0;
    var noLinkToCommitted = false;
    var triggeringFileName = FilenameUtils.getBaseName(bulkOperation.getLinkToTriggeringCsvFile());
    var committedRecordsMarcFile = String.format(CHANGED_MARC_PATH_TEMPLATE, bulkOperation.getId(), LocalDate.now(), triggeringFileName);
    var committedRecordsMarcCsvFile = String.format(CHANGED_MARC_CSV_PATH_TEMPLATE, bulkOperation.getId(), LocalDate.now(), triggeringFileName);
    if (!instanceIds.isEmpty()) {
      try (var writer = remoteFileSystemClient.marcWriter(committedRecordsMarcFile);
           var csvWriter = new CSVWriterBuilder(remoteFileSystemClient.writer(committedRecordsMarcCsvFile))
             .withSeparator(DEFAULT_SEPARATOR).build();) {
        while (fetchedNumOfRecords < instanceIds.size()) {
          var ids = instanceIds.stream()
            .skip(fetchedNumOfRecords)
            .limit(SRS_CHUNK_SIZE)
            .toList();
          var marcJsons = srsClient.getParsedRecordsInBatch(new GetParsedRecordsBatchRequestBody(
            new GetParsedRecordsBatchConditions(ids, "INSTANCE"), "MARC_BIB", true))
            .get("records");
          for (var jsonNodeIterator = marcJsons.elements(); jsonNodeIterator.hasNext();) {
            var recordJson = jsonNodeIterator.next().get("parsedRecord").get("content").toString();
            var marcRecord = jsonToMarcRecord(recordJson);
            writer.writeRecord(marcRecord);
            csvWriter.writeNext(marcCsvHelper.getModifiedDataForCsv(marcRecord));
          }
          fetchedNumOfRecords += ids.size();
          updateBulkOperationProgress(bulkOperation, ids.size());
        }
      } catch (IOException e) {
        log.error("Error updating MARC instances from SRS", e);
      }
    } else {
      noLinkToCommitted = true;
    }
    committedRecordsMarcFile = noLinkToCommitted ? null : committedRecordsMarcFile;
    completeBulkOperation(bulkOperation, fetchedNumOfRecords, committedRecordsMarcFile, committedRecordsMarcCsvFile);
  }

  private Record jsonToMarcRecord(String json) throws IOException {
    try (var inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
      var reader = new MarcJsonReader(inputStream);
      return reader.next();
    }
  }

  private void updateBulkOperationProgress(BulkOperation bulkOperation, int processedNumOfRecords) {
    var operation = bulkOperationRepository.findById(bulkOperation.getId())
      .orElseThrow(() -> new NotFoundException("BulkOperation was not found by id=" + bulkOperation.getId()));
    operation.setProcessedNumOfRecords(operation.getProcessedNumOfRecords() + processedNumOfRecords);
    bulkOperationRepository.save(operation);
  }

  private void completeBulkOperation(BulkOperation bulkOperation, int committedNumOfRecords, String committedRecordsMarcFile, String committedRecordsMarcCsvFile) {
    var operation = bulkOperationRepository.findById(bulkOperation.getId())
      .orElseThrow(() -> new NotFoundException("BulkOperation was not found by id=" + bulkOperation.getId()));
    operation.setLinkToCommittedRecordsMarcFile(committedRecordsMarcFile);
    operation.setLinkToCommittedRecordsMarcCsvFile(committedRecordsMarcCsvFile);
    operation.setTotalNumOfRecords(operation.getMatchedNumOfRecords());
    operation.setProcessedNumOfRecords(operation.getMatchedNumOfRecords());
    operation.setCommittedNumOfRecords(Math.max(committedNumOfRecords, getNumOfProcessedRecords(bulkOperation)));
    operation.setLinkToCommittedRecordsErrorsCsvFile(errorService.uploadErrorsToStorage(operation.getId()));
    operation.setCommittedNumOfErrors(errorService.getCommittedNumOfErrors(operation.getId()));
    operation.setCommittedNumOfWarnings(errorService.getCommittedNumOfWarnings(operation.getId()));
    operation.setEndTime(LocalDateTime.now());
    operation.setStatus(operation.getCommittedNumOfErrors() == 0 ? COMPLETED : COMPLETED_WITH_ERRORS);
    bulkOperationRepository.save(operation);
  }

  private int getNumOfProcessedRecords(BulkOperation bulkOperation) {
    return executionRepository.findAllByBulkOperationId(bulkOperation.getId()).stream()
      .map(BulkOperationExecution::getProcessedRecords)
      .mapToInt(v -> v)
      .max()
      .orElse(0);
  }
}
