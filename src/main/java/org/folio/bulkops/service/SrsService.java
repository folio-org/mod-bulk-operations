package org.folio.bulkops.service;

import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED;
import static org.folio.bulkops.domain.dto.OperationStatusType.COMPLETED_WITH_ERRORS;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.GetParsedRecordsBatchConditions;
import org.folio.bulkops.domain.bean.GetParsedRecordsBatchRequestBody;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.marc4j.MarcJsonReader;
import org.marc4j.marc.Record;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

  public void retrieveMarcInstancesFromSrs(List<String> instanceIds, BulkOperation bulkOperation) {
    var path = bulkOperation.getLinkToCommittedRecordsMarcFile();
    remoteFileSystemClient.remove(path);
    if (!instanceIds.isEmpty()) {
      var offset = 0;
      try (var writer = remoteFileSystemClient.marcWriter(path)) {
        while (offset < instanceIds.size()) {
          var ids = instanceIds.stream()
            .skip(offset)
            .limit(SRS_CHUNK_SIZE)
            .toList();
          var marcJsons = srsClient.getParsedRecordsInBatch(new GetParsedRecordsBatchRequestBody(
            new GetParsedRecordsBatchConditions(ids, "INSTANCE"), "MARC_BIB"))
            .get("records");
          for (var jsonNodeIterator = marcJsons.elements(); jsonNodeIterator.hasNext();) {
            var srsRec = jsonNodeIterator.next();
            var content = srsRec.get("parsedRecord").get("content").toString();
            writer.writeRecord(jsonToMarcRecord(content));
          }
          offset += ids.size();
          updateBulkOperationProgress(bulkOperation, offset);
        }
      } catch (IOException e) {
        log.error("Error updating MARC instances from SRS", e);
      }
    } else {
      bulkOperation.setLinkToCommittedRecordsMarcFile(null);
    }
    completeBulkOperation(bulkOperation);
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
    operation.setCommittedNumOfRecords(operation.getCommittedNumOfRecords() + processedNumOfRecords);
    bulkOperationRepository.save(operation);
  }

  private void completeBulkOperation(BulkOperation bulkOperation) {
    var operation = bulkOperationRepository.findById(bulkOperation.getId())
      .orElseThrow(() -> new NotFoundException("BulkOperation was not found by id=" + bulkOperation.getId()));
    operation.setTotalNumOfRecords(operation.getMatchedNumOfRecords());
    operation.setProcessedNumOfRecords(operation.getMatchedNumOfRecords());
    operation.setLinkToCommittedRecordsErrorsCsvFile(errorService.uploadErrorsToStorage(operation.getId()));
    operation.setCommittedNumOfErrors(errorService.getCommittedNumOfErrors(operation.getId()));
    operation.setEndTime(LocalDateTime.now());
    operation.setStatus(operation.getCommittedNumOfErrors() == 0 ? COMPLETED : COMPLETED_WITH_ERRORS);
    bulkOperationRepository.save(operation);
  }
}
