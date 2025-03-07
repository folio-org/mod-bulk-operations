package org.folio.bulkops.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_HRID;
import static org.folio.bulkops.service.BulkOperationService.CHANGED_CSV_PATH_TEMPLATE;
import static org.folio.bulkops.service.MarcUpdateService.CHANGED_MARC_PATH_TEMPLATE;
import static org.folio.bulkops.util.Constants.INSTANCE_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;
import static org.folio.bulkops.util.Constants.LINE_BREAK;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.converter.BulkOperationsEntityCsvWriter;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.Marc21ReferenceProvider;
import org.folio.bulkops.service.MarcToUnifiedTableRowMapper;
import org.folio.bulkops.service.NoteTableUpdater;
import org.folio.bulkops.service.RuleService;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.marc.Record;
import org.springframework.stereotype.Component;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Log4j2
public class MarcCsvHelper {
  public static final String ENRICHED_PREFIX = "enriched-";

  private final NoteTableUpdater noteTableUpdater;
  private final MarcToUnifiedTableRowMapper marcToUnifiedTableRowMapper;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final RuleService ruleService;
  private final Marc21ReferenceProvider marc21ReferenceProvider;
  private final ObjectMapper objectMapper;

  public String[] getModifiedDataForCsv(Record marcRecord) {
    var instanceHeaderNames = getInstanceHeaderNames();
    var csvData = marcToUnifiedTableRowMapper.processRecord(marcRecord, instanceHeaderNames, true);
    var concatenatedNotes = csvData.subList(INSTANCE_NOTE_POSITION, csvData.size()).stream()
      .filter(StringUtils::isNotBlank)
      .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
    csvData = csvData.subList(0, INSTANCE_NOTE_POSITION + 1);
    csvData.set(INSTANCE_NOTE_POSITION, concatenatedNotes);
    return csvData.toArray(String[]::new);
  }

  public byte[] enrichCsvWithMarcChanges(byte[] content, BulkOperation bulkOperation) {
    var instanceHeaderNames = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Instance.class).getHeader().stream()
      .map(Cell::getValue)
      .toList();
    var changedMarcData = getChangedMarcData(bulkOperation);
    if (!changedMarcData.isEmpty()) {
      try (var reader = new CSVReaderBuilder(new InputStreamReader(new ByteArrayInputStream(content)))
        .withCSVParser(CSVHelper.getCsvParser()).build();
        var writer = new StringWriter()) {
        String[] line;
        while ((line = reader.readNext()) != null) {
          var hrid = line[instanceHeaderNames.indexOf(INSTANCE_HRID)];
          if (changedMarcData.containsKey(hrid)) {
            for (var entry : changedMarcData.get(hrid).entrySet()) {
              line[instanceHeaderNames.indexOf(entry.getKey())] = entry.getValue();
            }
          }
          handleSpecialCharacters(line);
          writer.write(String.join(",", line) + "\n");
        }
        writer.flush();
        return writer.toString().getBytes();
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    }
    return content;
  }

  private Map<String, Map<String, String>> getChangedMarcData(BulkOperation bulkOperation) {
    var instanceHeaderNames = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Instance.class).getHeader().stream()
      .map(Cell::getValue)
      .toList();
    Map<String, Map<String, String>> result = new HashMap<>();
    var fileName = getCsvFileName(bulkOperation);
    if (nonNull(fileName)) {
      try (var reader = new CSVReaderBuilder(new InputStreamReader(remoteFileSystemClient.get(fileName)))
        .withCSVParser(CSVHelper.getCsvParser()).build()) {
        marc21ReferenceProvider.updateMappingRules();
        var changedOptionsSet = marc21ReferenceProvider.getChangedOptionsSetForCsv(ruleService.getMarcRules(bulkOperation.getId()));
        String[] line;
        while ((line = reader.readNext()) != null) {
          if (line.length == instanceHeaderNames.size()) {
            var hrid = line[instanceHeaderNames.indexOf(INSTANCE_HRID)];
            Map<String, String> changedValues = new HashMap<>();
            for (var option : changedOptionsSet) {
              var index = instanceHeaderNames.indexOf(option);
              if (index != -1) {
                changedValues.put(option, line[index]);
              }
            }
            result.put(hrid, changedValues);
          }
        }
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    }
    return result;
  }

  private String getCsvFileName(BulkOperation bulkOperation) {
    return switch (bulkOperation.getStatus()) {
      case REVIEW_CHANGES -> bulkOperation.getLinkToModifiedRecordsMarcCsvFile();
      case COMPLETED, COMPLETED_WITH_ERRORS -> bulkOperation.getLinkToCommittedRecordsMarcCsvFile();
      default -> null;
    };
  }

  private List<String> getInstanceHeaderNames() {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Instance.class);
    noteTableUpdater.extendTableWithInstanceNotesTypes(table, Collections.emptySet());
    return table.getHeader().stream()
      .map(Cell::getValue)
      .toList();
  }

  private void handleSpecialCharacters(String[] strings) {
    for (int i = 0; i < strings.length; i++) {
      var s = strings[i];
      if (s.contains("\"")) {
        s = s.replace("\"", "\"\"");
      }
      if (s.contains("\n") || s.contains(",")) {
        s = "\"" + s + "\"";
      }
      strings[i] = s;
    }
  }

  public void enrichMarcAndCsvCommittedFiles(BulkOperation bulkOperation) {
    var csvHrids = getUpdatedInventoryInstanceHrids(bulkOperation);
    var marcHrids = getUpdatedMarcInstanceHrids(bulkOperation);
    enrichCommittedCsvWithUpdatedMarcRecords(bulkOperation, csvHrids, marcHrids);
    enrichCommittedMarcWithUpdatedInventoryRecords(bulkOperation, csvHrids, marcHrids);
  }

  public void enrichCommittedCsvWithUpdatedMarcRecords(BulkOperation bulkOperation, List<String> csvHrids, List<String> marcHrids) {
    var hrids = CollectionUtils.subtract(marcHrids, csvHrids);
    if (!hrids.isEmpty() && nonNull(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
      var dirName = isNull(bulkOperation.getLinkToCommittedRecordsCsvFile()) ? bulkOperation.getId() : ENRICHED_PREFIX + bulkOperation.getId();
      var committedCsvFileName = CHANGED_CSV_PATH_TEMPLATE.formatted(dirName, LocalDate.now(), getBaseName(bulkOperation.getLinkToTriggeringCsvFile()));
      try (var matchedFileReader = new InputStreamReader(new BufferedInputStream(remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsJsonFile())));
           var writer = isNull(bulkOperation.getLinkToCommittedRecordsCsvFile()) ?
             remoteFileSystemClient.writer(committedCsvFileName) : new StringWriter()) {
        var matchedFileParser = new JsonFactory().createParser(matchedFileReader);
        var matchedFileIterator = objectMapper.readValues(matchedFileParser, ExtendedInstance.class);
        var csvWriter = new BulkOperationsEntityCsvWriter(writer, Instance.class);
        while (matchedFileIterator.hasNext()) {
          var instance = matchedFileIterator.next().getEntity();
          if (hrids.contains(instance.getHrid())) {
            csvWriter.write(instance);
          }
        }
        if (nonNull(bulkOperation.getLinkToCommittedRecordsCsvFile())) {
          var appendedCsvRecords = writer.toString();
          appendedCsvRecords = appendedCsvRecords.substring(appendedCsvRecords.indexOf(LINE_BREAK) + 1);
          var appendedCsvStream = new SequenceInputStream(
            remoteFileSystemClient.get(bulkOperation.getLinkToCommittedRecordsCsvFile()),
            new ByteArrayInputStream(appendedCsvRecords.getBytes()));
          remoteFileSystemClient.put(appendedCsvStream, committedCsvFileName);
        }
      } catch (Exception e) {
        log.error("Failed to enrich csv file", e);
      } finally {
        if (nonNull(bulkOperation.getLinkToCommittedRecordsCsvFile())) {
          remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsCsvFile());
        }
        bulkOperation.setLinkToCommittedRecordsCsvFile(committedCsvFileName);
      }
    }
  }

  public void enrichCommittedMarcWithUpdatedInventoryRecords(BulkOperation bulkOperation, List<String> csvHrids, List<String> marcHrids) {
    var hrids = CollectionUtils.subtract(csvHrids, marcHrids);
    if (!hrids.isEmpty() && nonNull(bulkOperation.getLinkToMatchedRecordsMarcFile())) {
      var linkToCommitted = bulkOperation.getLinkToCommittedRecordsMarcFile();
      var dirName = isNull(linkToCommitted) ? bulkOperation.getId() : ENRICHED_PREFIX + bulkOperation.getId();
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
      } catch (Exception e) {
        log.error("Failed to enrich marc file", e);
      } finally {
        if (nonNull(bulkOperation.getLinkToCommittedRecordsMarcFile())) {
          remoteFileSystemClient.remove(bulkOperation.getLinkToCommittedRecordsMarcFile());
        }
        bulkOperation.setLinkToCommittedRecordsMarcFile(committedMarcFileName);
      }
    }
  }

  public List<String> getUpdatedInventoryInstanceHrids(BulkOperation bulkOperation) {
    List<String> updatedHrids = new ArrayList<>();
    if (nonNull(bulkOperation.getLinkToCommittedRecordsCsvFile())) {
      var instanceHeaderNames = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Instance.class).getHeader().stream()
        .map(Cell::getValue)
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
