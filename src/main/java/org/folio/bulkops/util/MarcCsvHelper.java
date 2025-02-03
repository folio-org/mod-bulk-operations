package org.folio.bulkops.util;

import static java.util.Objects.nonNull;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_HRID;
import static org.folio.bulkops.util.Constants.INSTANCE_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;

import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.Marc21ReferenceProvider;
import org.folio.bulkops.service.MarcToUnifiedTableRowMapper;
import org.folio.bulkops.service.NoteTableUpdater;
import org.folio.bulkops.service.RuleService;
import org.marc4j.marc.Record;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Log4j2
public class MarcCsvHelper {
  private final NoteTableUpdater noteTableUpdater;
  private final MarcToUnifiedTableRowMapper marcToUnifiedTableRowMapper;
  private final RemoteFileSystemClient remoteFileSystemClient;
  private final RuleService ruleService;
  private final Marc21ReferenceProvider marc21ReferenceProvider;

  public String[] getModifiedDataForCsv(Record record) {
    var instanceHeaderNames = getInstanceHeaderNames();
    var csvData = marcToUnifiedTableRowMapper.processRecord(record, instanceHeaderNames, true);
    var concatenatedNotes = csvData.subList(INSTANCE_NOTE_POSITION, csvData.size()).stream()
      .filter(StringUtils::isNotBlank)
      .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
    csvData = csvData.subList(0, INSTANCE_NOTE_POSITION + 1);
    csvData.set(INSTANCE_NOTE_POSITION, concatenatedNotes);
    return csvData.toArray(String[]::new);
  }

  public byte[] enrichCsvWithMarcChanges(byte[] content, BulkOperation bulkOperation) {
    var instanceHeaderNames = getInstanceHeaderNames();
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
    var instanceHeaderNames = getInstanceHeaderNames();
    Map<String, Map<String, String>> result = new HashMap<>();
    var fileName = getCsvFileName(bulkOperation);
    if (nonNull(fileName)) {
      try (var reader = new CSVReaderBuilder(new InputStreamReader(remoteFileSystemClient.get(fileName)))
        .withCSVParser(CSVHelper.getCsvParser()).build()) {
        var changedOptionsSet = marc21ReferenceProvider.getChangedOptionsSet(ruleService.getMarcRules(bulkOperation.getId()));
        String[] line;
        while ((line = reader.readNext()) != null) {
          if (line.length == instanceHeaderNames.size()) {
            var hrid = line[instanceHeaderNames.indexOf(INSTANCE_HRID)];
            Map<String, String> changedValues = new HashMap<>();
            for (var entry : changedOptionsSet) {
              var index = instanceHeaderNames.indexOf(entry);
              if (index != -1) {
                changedValues.put(entry, line[index]);
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
}
