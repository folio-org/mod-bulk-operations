package org.folio.bulkops.processor.marc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_HRID;
import static org.folio.bulkops.util.Constants.CHANGED_CSV_PATH_TEMPLATE;
import static org.folio.bulkops.util.Constants.CHANGED_MARC_PATH_TEMPLATE;
import static org.folio.bulkops.util.Constants.ENRICHED_PREFIX;
import static org.folio.bulkops.util.Constants.LINE_BREAK;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.util.BulkOperationsEntityCsvWriter;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.ConverterException;
import org.folio.bulkops.processor.CommitProcessor;
import org.folio.bulkops.util.CSVHelper;
import org.folio.bulkops.util.UnifiedTableHeaderBuilder;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.springframework.stereotype.Service;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class MarcFlowCommitProcessor implements CommitProcessor {
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final ObjectMapper objectMapper;

  @Override
  public void processCommit(BulkOperation bulkOperation) {
    var csvHrids = getUpdatedInventoryInstanceHrids(bulkOperation);
    var marcHrids = getUpdatedMarcInstanceHrids(bulkOperation);

    var updatedHrids = new HashSet<>(csvHrids);
    updatedHrids.addAll(marcHrids);
    bulkOperation.setCommittedNumOfRecords(updatedHrids.size());

    enrichCommittedCsvWithUpdatedMarcRecords(bulkOperation, csvHrids, marcHrids);
    enrichCommittedMarcWithUpdatedInventoryRecords(bulkOperation, csvHrids, marcHrids);
  }

  public void enrichCommittedCsvWithUpdatedMarcRecords(BulkOperation bulkOperation, List<String> csvHrids, List<String> marcHrids) {
    var hrids = CollectionUtils.subtract(marcHrids, csvHrids);
    if (!hrids.isEmpty() && nonNull(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
      var dirName = isNull(bulkOperation.getLinkToCommittedRecordsCsvFile()) ? bulkOperation.getId().toString() : ENRICHED_PREFIX + bulkOperation.getId();
      var committedCsvFileName = CHANGED_CSV_PATH_TEMPLATE.formatted(dirName, LocalDate.now(), getBaseName(bulkOperation.getLinkToTriggeringCsvFile()));
      try (var matchedFileReader = new InputStreamReader(new BufferedInputStream(remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsJsonFile())));
           var writer = isNull(bulkOperation.getLinkToCommittedRecordsCsvFile()) ? remoteFileSystemClient.writer(committedCsvFileName) : new StringWriter()) {
        var matchedFileParser = new JsonFactory().createParser(matchedFileReader);
        var matchedFileIterator = objectMapper.readValues(matchedFileParser, ExtendedInstance.class);
        var csvWriter = new BulkOperationsEntityCsvWriter(writer, Instance.class);
        while (matchedFileIterator.hasNext()) {
          var instance = matchedFileIterator.next().getEntity();
          if (hrids.contains(instance.getHrid())) {
            writeBeanToCsv(csvWriter, instance);
          }
        }
        if (nonNull(bulkOperation.getLinkToCommittedRecordsCsvFile())) {
          var appendedCsvRecords = writer.toString();
          appendedCsvRecords = appendedCsvRecords.substring(appendedCsvRecords.indexOf(LINE_BREAK) + 1);
          var appendedCsvStream = new SequenceInputStream(
            remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsCsvFile()),
            new ByteArrayInputStream(appendedCsvRecords.getBytes()));
          remoteFileSystemClient.put(appendedCsvStream, committedCsvFileName);
          remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsCsvFile());
        }
        bulkOperation.setLinkToCommittedRecordsCsvFile(committedCsvFileName);
      } catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
        log.error("Failed to enrich csv file", e);
      }
    }
  }

  private void writeBeanToCsv(BulkOperationsEntityCsvWriter writer, BulkOperationsEntity bean)
    throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
    try {
      writer.write(bean);
    } catch (ConverterException e) {
      writeBeanToCsv(writer, bean);
    }
  }

  public void enrichCommittedMarcWithUpdatedInventoryRecords(BulkOperation bulkOperation, List<String> csvHrids, List<String> marcHrids) {
    var hrids = CollectionUtils.subtract(csvHrids, marcHrids);
    if (!hrids.isEmpty() && nonNull(bulkOperation.getLinkToMatchedRecordsMarcFile())) {
      var linkToCommitted = bulkOperation.getLinkToCommittedRecordsMarcFile();
      var dirName = isNull(linkToCommitted) ? bulkOperation.getId().toString() : ENRICHED_PREFIX + bulkOperation.getId();
      var committedMarcFileName = CHANGED_MARC_PATH_TEMPLATE.formatted(dirName, LocalDate.now(), getBaseName(bulkOperation.getLinkToTriggeringCsvFile()));
      try (var matchedMarcInputStream = remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsMarcFile());
           var committedMarcInputStream = isNull(linkToCommitted) ? null : remoteFileSystemClient.get(linkToCommitted);
           var marcOutputStream = new ByteArrayOutputStream()) {
        var marcReader = new MarcStreamReader(matchedMarcInputStream);
        var streamWriter = new MarcStreamWriter(marcOutputStream, "UTF-8");
        while (marcReader.hasNext()) {
          var marcRecord = marcReader.next();
          if (hrids.contains(marcRecord.getControlNumber())) {
            streamWriter.write(marcRecord);
          }
        }
        var appendedMarcStream = new ByteArrayInputStream(marcOutputStream.toString(UTF_8).getBytes());
        var committedMarcStream = isNull(linkToCommitted) ?
          appendedMarcStream :
          new SequenceInputStream(committedMarcInputStream, appendedMarcStream);
        remoteFileSystemClient.put(committedMarcStream, committedMarcFileName);
        if (nonNull(bulkOperation.getLinkToCommittedRecordsMarcFile())) {
          remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsMarcFile());
        }
        bulkOperation.setLinkToCommittedRecordsMarcFile(committedMarcFileName);
      } catch (IOException e) {
        log.error("Failed to enrich marc file", e);
      }
    }
  }

  public List<String> getUpdatedInventoryInstanceHrids(BulkOperation bulkOperation) {
    List<String> updatedHrids = new ArrayList<>();
    if (nonNull(bulkOperation.getLinkToCommittedRecordsCsvFile())) {
      var instanceHeaderNames = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Instance.class).getHeader().stream()
        .map(org.folio.bulkops.domain.dto.Cell::getValue)
        .toList();
      try (var reader = new CSVReaderBuilder(new InputStreamReader(remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsCsvFile())))
        .withCSVParser(CSVHelper.getCsvParser()).withSkipLines(1).build()) {
        String[] line;
        while ((line = reader.readNext()) != null) {
          if (line.length == instanceHeaderNames.size()) {
            updatedHrids.add(line[instanceHeaderNames.indexOf(INSTANCE_HRID)]);
          }
        }
      } catch (IOException | CsvValidationException e) {
        log.error("Failed to read csv file", e);
      }
    }
    return updatedHrids;
  }

  public List<String> getUpdatedMarcInstanceHrids(BulkOperation bulkOperation) {
    List<String> updatedHrids = new ArrayList<>();
    if (nonNull(bulkOperation.getLinkToCommittedRecordsMarcFile())) {
      try (var marcInputStream = remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsMarcFile())) {
        var marcReader = new MarcStreamReader(marcInputStream);
        while (marcReader.hasNext()) {
          var marcRecord = marcReader.next();
          updatedHrids.add(marcRecord.getControlNumber());
        }
      } catch (IOException e) {
        log.error("Failed to read marc file", e);
      }
    }
    return updatedHrids;
  }
}
