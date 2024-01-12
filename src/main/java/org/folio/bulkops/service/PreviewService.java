package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.folio.bulkops.util.Constants.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.util.Constants.ADMINISTRATIVE_NOTES;
import static org.folio.bulkops.util.Utils.resolveEntityClass;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.client.HoldingsNoteTypeClient;
import org.folio.bulkops.client.ItemNoteTypeClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.domain.dto.UpdateActionType;
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
  private final ItemNoteTypeClient itemNoteTypeClient;
  private final HoldingsNoteTypeClient holdingsNoteTypeClient;

  private static final Pattern UUID_REGEX =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  public UnifiedTable getPreview(BulkOperation operation, BulkOperationStep step, int offset, int limit) {
    var clazz = resolveEntityClass(operation.getEntityType());
    return switch (step) {
      case UPLOAD -> buildPreviewFromCsvFile(operation.getLinkToMatchedRecordsCsvFile(), clazz, offset, limit);
      case EDIT -> {
        var bulkOperationId = operation.getId();
        var rules = ruleService.getRules(bulkOperationId);
        var options = getChangedOptions(bulkOperationId, rules, clazz);
        yield buildPreviewFromCsvFile(operation.getLinkToModifiedRecordsCsvFile(), clazz, offset, limit, options);
      }
      case COMMIT -> buildPreviewFromCsvFile(operation.getLinkToCommittedRecordsCsvFile(), clazz, offset, limit);
    };
  }

  private Set<String> getChangedOptions(UUID bulkOperationId, BulkOperationRuleCollection rules, Class<? extends BulkOperationsEntity> clazz) {
    Set<String> options = new HashSet<>();
    rules.getBulkOperationRules().forEach(rule -> {
      var option = rule.getRuleDetails().getOption();
      rule.getRuleDetails().getActions().forEach(action -> {
        var updated = action.getUpdated();
        if (action.getType() == UpdateActionType.DUPLICATE) {
          options.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(option));
          options.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(UpdateOptionType.fromValue(updated)));
        } else if (action.getType() == UpdateActionType.CHANGE_TYPE) {
          if (UUID_REGEX.matcher(updated).matches()) {
            if (clazz == HoldingsRecord.class) {
              var holdingNoteType = holdingsNoteTypeClient.getById(updated).getName();
              options.add(holdingNoteType);
            } else if (clazz == Item.class) {
              var itemNoteType = itemNoteTypeClient.getById(updated).getName();
              options.add(itemNoteType);
            }
          } else {
            options.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(UpdateOptionType.fromValue(updated)));
          }
        } else {
          options.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(option));
        }
      });
    });
    log.info(format("Bulk Operation ID: %s, forced to visible fields: %s", bulkOperationId.toString(), String.join(",", options)));
    return options;
  }

  private UnifiedTable buildPreviewFromCsvFile(String pathToFile, Class<? extends BulkOperationsEntity> clazz, int offset, int limit, Set<String> forceVisible) {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(clazz, forceVisible);
    return populatePreview(pathToFile, clazz, offset, limit, table, forceVisible);
  }

  private UnifiedTable buildPreviewFromCsvFile(String pathToFile, Class<? extends BulkOperationsEntity> clazz, int offset, int limit) {
    var table =  UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(clazz);
    return populatePreview(pathToFile, clazz, offset, limit, table, emptySet());
  }

  private UnifiedTable populatePreview(String pathToFile, Class<? extends BulkOperationsEntity> clazz, int offset, int limit, UnifiedTable table, Set<String> forceVisible) {
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
      processNoteFields(table, clazz, forceVisible);
      table.getRows().forEach(row -> row.setRow(SpecialCharacterEscaper.restore(row.getRow())));
      return table;
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return table;
  }

  private void processNoteFields(UnifiedTable table, Class<? extends BulkOperationsEntity> clazz, Set<String> forceVisible) {
    if (clazz == Item.class) {
      noteTableUpdater.extendTableWithItemNotesTypes(table, forceVisible);
    }
    if (clazz == HoldingsRecord.class) {
      noteTableUpdater.extendTableWithHoldingsNotesTypes(table, forceVisible);
    }
  }
}
