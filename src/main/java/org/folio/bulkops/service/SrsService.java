package org.folio.bulkops.service;

import static com.opencsv.ICSVWriter.DEFAULT_SEPARATOR;
import static java.util.Objects.nonNull;
import static org.folio.bulkops.util.Constants.CHANGED_MARC_CSV_PATH_TEMPLATE;
import static org.folio.bulkops.util.Constants.CHANGED_MARC_PATH_TEMPLATE;
import static org.folio.bulkops.util.Constants.MULTIPLE_SRS;
import static org.folio.bulkops.util.Constants.SRS_MISSING;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVWriterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.domain.bean.GetParsedRecordsBatchConditions;
import org.folio.bulkops.domain.bean.GetParsedRecordsBatchRequestBody;
import org.folio.bulkops.domain.converter.JsonToMarcConverter;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.MarcValidationException;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.MarcCsvHelper;
import org.folio.bulkops.util.MarcValidator;
import org.marc4j.MarcJsonReader;
import org.marc4j.marc.Record;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Log4j2
public class SrsService {
  private static final int SRS_CHUNK_SIZE = 100;

  private final SrsClient srsClient;
  private final BulkOperationRepository bulkOperationRepository;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final MarcCsvHelper marcCsvHelper;
  private final BulkOperationServiceHelper bulkOperationServiceHelper;
  private final JsonToMarcConverter jsonToMarcConverter;

  public void retrieveMarcInstancesFromSrs(List<String> instanceIds, BulkOperation bulkOperation) {
    log.info("Retrieving {} marc instances from SRS", instanceIds.size());
    var fetchedNumOfRecords = 0;
    var noLinkToCommitted = false;
    var triggeringFileName = FilenameUtils.getBaseName(bulkOperation.getLinkToTriggeringCsvFile());
    var committedRecordsMarcFile = String.format(CHANGED_MARC_PATH_TEMPLATE, bulkOperation.getId(), LocalDate.now(), triggeringFileName);
    var committedRecordsMarcCsvFile = String.format(CHANGED_MARC_CSV_PATH_TEMPLATE, bulkOperation.getId(), LocalDate.now(), triggeringFileName);
    if (!instanceIds.isEmpty()) {
      var path = bulkOperation.getLinkToCommittedRecordsMarcFile();
      if (nonNull(path)) {
        remoteFileSystemClient.remove(path);
        bulkOperation.setLinkToCommittedRecordsMarcFile(null);
      }
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
    bulkOperation.setLinkToCommittedRecordsMarcFile(committedRecordsMarcFile);
    bulkOperation.setLinkToCommittedRecordsMarcCsvFile(committedRecordsMarcCsvFile);
    bulkOperationServiceHelper.completeBulkOperation(bulkOperation);
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

  public String getMarcJsonString(String instanceId) throws MarcValidationException, IOException {
    var srsRecords = srsClient.getMarc(instanceId, "INSTANCE", true).get("sourceRecords");
    if (srsRecords.isEmpty()) {
      throw new MarcValidationException(SRS_MISSING);
    } else if (srsRecords.size() > 1) {
      throw new MarcValidationException(MULTIPLE_SRS.formatted(String.join(", ", getAllSrsIds(srsRecords))));
    } else {
      var srsRec = srsRecords.elements().next();
      var parsedRec = srsRec.get("parsedRecord");
      var marcJsonString = parsedRec.get("content").toString();
      MarcValidator.validate(marcJsonString);
      return jsonToMarcConverter.convertJsonRecordToMarcRecord(marcJsonString);
    }
  }

  private String getAllSrsIds(JsonNode srsRecords) {
    return String.join(", ", StreamSupport.stream(srsRecords.spliterator(), false)
      .map(n -> StringUtils.strip(n.get("recordId").toString(), "\"")).toList());
  }
}
