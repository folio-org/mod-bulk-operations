package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.util.Constants.ADMINISTRATIVE_NOTES;
import static org.folio.bulkops.util.Utils.resolveEntityClass;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.util.UnifiedTableHeaderBuilder;
import org.folio.bulkops.util.UpdateOptionTypeToFieldResolver;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class PreviewService {

  private final RuleService ruleService;
  private final NoteTableUpdater noteTableUpdater;
  private final RemoteFileSystemClient remoteFileSystemClient;

  public UnifiedTable getPreview(BulkOperation operation, BulkOperationStep step, int offset, int limit) {
    var clazz = resolveEntityClass(operation.getEntityType());
    return switch (step) {
      case UPLOAD -> buildPreviewFromCsvFile(operation.getLinkToMatchedRecordsCsvFile(), clazz, offset, limit);
      case EDIT -> {
        var rules = ruleService.getRules(operation.getId());
        var options = getOptionsList(rules);
        var fields = UpdateOptionTypeToFieldResolver.getFieldsByUpdateOptionTypes(options);
        yield buildPreviewFromCsvFile(operation.getLinkToModifiedRecordsCsvFile(), clazz, offset, limit, fields);
      }
      case COMMIT -> buildPreviewFromCsvFile(operation.getLinkToCommittedRecordsCsvFile(), clazz, offset, limit);
    };
  }

  private List<UpdateOptionType> getOptionsList(BulkOperationRuleCollection rules) {
    return rules.getBulkOperationRules().stream().map(rule -> rule.getRuleDetails().getOption()).toList();
  }

  private UnifiedTable buildPreviewFromCsvFile(String pathToFile, Class<? extends BulkOperationsEntity> clazz, int offset, int limit, List<String> forceVisible) {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(clazz, forceVisible);
    return populatePreview(pathToFile, clazz, offset, limit, table);
  }

  private UnifiedTable buildPreviewFromCsvFile(String pathToFile, Class<? extends BulkOperationsEntity> clazz, int offset, int limit) {
    var table =  UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(clazz);
    return populatePreview(pathToFile, clazz, offset, limit, table);
  }

  private UnifiedTable populatePreview(String pathToFile, Class<? extends BulkOperationsEntity> clazz, int offset, int limit, org.folio.bulkops.domain.dto.UnifiedTable table) {
    try (Reader reader = new InputStreamReader(remoteFileSystemClient.get(pathToFile))) {
      try (CSVReader csvReader = new CSVReader(reader)) {
        var recordsToSkip = offset + 1;
        csvReader.skip(recordsToSkip);
        String[] line;
        while ((line = csvReader.readNext()) != null && csvReader.getRecordsRead() <= limit + recordsToSkip) {
          var row = new org.folio.bulkops.domain.dto.Row();
          row.setRow(new ArrayList<>(Arrays.asList(line)));
          table.addRowsItem(row);
        }
      }
      processNoteFields(table, clazz);
      table.getRows().forEach(row -> row.setRow(SpecialCharacterEscaper.restore(row.getRow())));
      return table;
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return table;
  }

  private void processNoteFields(UnifiedTable table, Class<? extends BulkOperationsEntity> clazz) {
    table.getHeader().forEach(cell -> {
      if (ADMINISTRATIVE_NOTES.equalsIgnoreCase(cell.getValue())) {
        cell.setValue(ADMINISTRATIVE_NOTE);
      }
    });
    if (clazz == Item.class) {
      noteTableUpdater.extendTableWithItemNotesTypes(table);
    }
    if (clazz == HoldingsRecord.class) {
      noteTableUpdater.extendTableWithHoldingsNotesTypes(table);
    }
  }
}
