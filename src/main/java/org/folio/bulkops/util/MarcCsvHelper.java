package org.folio.bulkops.util;

import static java.util.Objects.nonNull;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_CLASSIFICATION;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_ELECTRONIC_ACCESS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_PUBLICATION;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_HRID;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SUBJECT;
import static org.folio.bulkops.util.Constants.INSTANCE_CLASSIFICATION_POSITION;
import static org.folio.bulkops.util.Constants.INSTANCE_PUBLICATION_POSITION;
import static org.folio.bulkops.util.Constants.INSTANCE_ELECTRONIC_ACCESS_POSITION;
import static org.folio.bulkops.util.Constants.INSTANCE_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.INSTANCE_SUBJECT_POSITION;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;

import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ObjectMapper objectMapper;

  public String[] getModifiedDataForCsv(Record marcRecord) {
    var instanceHeaderNames = getInstanceHeaderNames();
    var csvData = marcToUnifiedTableRowMapper.processRecord(marcRecord, instanceHeaderNames, true);
    var concatenatedNotes = csvData.subList(INSTANCE_NOTE_POSITION, instanceHeaderNames.indexOf(INSTANCE_ELECTRONIC_ACCESS)).stream()
      .filter(StringUtils::isNotBlank)
      .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
    var electronicAccessData = csvData.get(instanceHeaderNames.indexOf(INSTANCE_ELECTRONIC_ACCESS));
    var subjectData = csvData.get(instanceHeaderNames.indexOf(INSTANCE_SUBJECT));
    var publicationData = csvData.get(instanceHeaderNames.indexOf(INSTANCE_PUBLICATION));
    var classificationData = csvData.get(instanceHeaderNames.indexOf(INSTANCE_CLASSIFICATION));
    csvData.set(INSTANCE_NOTE_POSITION, concatenatedNotes);
    csvData.set(INSTANCE_ELECTRONIC_ACCESS_POSITION, electronicAccessData);
    csvData.set(INSTANCE_SUBJECT_POSITION, subjectData);
    csvData.set(INSTANCE_PUBLICATION_POSITION, publicationData);
    csvData.set(INSTANCE_CLASSIFICATION_POSITION, classificationData);
    csvData = csvData.subList(0, INSTANCE_CLASSIFICATION_POSITION + 1);
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
          if (StringUtils.isNotBlank(hrid) && changedMarcData.containsKey(hrid)) {
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
              if (index != -1 && nonNull(line[index])) {
                changedValues.put(option, line[index]);
              }
            }
            if (!changedValues.isEmpty()) {
              result.put(hrid, changedValues);
            }
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
      case REVIEW_CHANGES, COMPLETED, COMPLETED_WITH_ERRORS ->
              bulkOperation.getLinkToModifiedRecordsMarcCsvFile();
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
}
