package org.folio.bulkops.service;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.folio.bulkops.domain.dto.ApproachType.MANUAL;
import static org.folio.bulkops.domain.dto.UpdateOptionType.HOLDINGS_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ITEM_NOTE;
import static org.folio.bulkops.processor.HoldingsNotesUpdater.HOLDINGS_NOTE_TYPE_ID_KEY;
import static org.folio.bulkops.processor.ItemsNotesUpdater.ITEM_NOTE_TYPE_ID_KEY;
import static org.folio.bulkops.util.Utils.resolveEntityClass;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.Parameter;
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
import org.folio.bulkops.domain.dto.EntityType;

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
    var entityType = operation.getEntityType();
    var clazz = resolveEntityClass(operation.getEntityType());
    return switch (step) {
      case UPLOAD -> buildPreviewFromCsvFile(operation.getLinkToMatchedRecordsCsvFile(), clazz, offset, limit);
      case EDIT -> {
        var bulkOperationId = operation.getId();
        var rules = ruleService.getRules(bulkOperationId);
        var options = getChangedOptionsSet(bulkOperationId, entityType, rules, clazz);
        yield buildPreviewFromCsvFile(operation.getLinkToModifiedRecordsCsvFile(), clazz, offset, limit, options);
      }
      case COMMIT -> {
        if (MANUAL == operation.getApproach()) {
          yield buildPreviewFromCsvFile(operation.getLinkToCommittedRecordsCsvFile(), clazz, offset, limit);
        } else {
          var bulkOperationId = operation.getId();
          var rules = ruleService.getRules(bulkOperationId);
          var options = getChangedOptionsSet(bulkOperationId, entityType, rules, clazz);
          yield buildPreviewFromCsvFile(operation.getLinkToCommittedRecordsCsvFile(), clazz, offset, limit, options);
        }
      }
    };
  }

  private Set<String> getChangedOptionsSet(UUID bulkOperationId, EntityType entityType, BulkOperationRuleCollection rules, Class<? extends BulkOperationsEntity> clazz) {
    Set<String> forceVisibleOptions = new HashSet<>();
    rules.getBulkOperationRules().forEach(rule -> {
      var option = rule.getRuleDetails().getOption();
      rule.getRuleDetails().getActions().forEach(action -> {

        if (action.getType() == UpdateActionType.CHANGE_TYPE) {

          Optional<String> initial = Optional.empty();
          if (EntityType.ITEM == entityType) {
            initial = CollectionUtils.isNotEmpty(action.getParameters()) ? action.getParameters().stream()
              .filter(p -> ITEM_NOTE_TYPE_ID_KEY.equals(p.getKey())).map(Parameter::getValue).findFirst() : Optional.empty();
          }

          if (EntityType.HOLDINGS_RECORD == entityType) {
            initial = CollectionUtils.isNotEmpty(action.getParameters()) ? action.getParameters().stream()
              .filter(p -> HOLDINGS_NOTE_TYPE_ID_KEY.equals(p.getKey())).map(Parameter::getValue).findFirst() : Optional.empty();
          }

          if (initial.isPresent()) {
            var type = resolveAndGetItemTypeById(clazz, initial.get());
            if (StringUtils.isNotEmpty(type)) {
              forceVisibleOptions.add(type);
            }
          } else {
            forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(option, entityType));
          }

          var updated = action.getUpdated();
          if (UUID_REGEX.matcher(updated).matches()) {
            var type = resolveAndGetItemTypeById(clazz, updated);
            if (StringUtils.isNotEmpty(type)) {
              forceVisibleOptions.add(type);
            }
          } else {
            forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(UpdateOptionType.fromValue(updated), entityType));
          }
        } else if (action.getType() == UpdateActionType.DUPLICATE) {
          forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(option, entityType));
          forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(UpdateOptionType.fromValue(action.getUpdated()), entityType));
        } else if (ITEM_NOTE == option) {
          var initial = action.getParameters().stream().filter(p -> ITEM_NOTE_TYPE_ID_KEY.equals(p.getKey())).map(Parameter::getValue).findFirst();
          initial.ifPresent(id -> {
            var type = resolveAndGetItemTypeById(clazz, id);
            if (StringUtils.isNotEmpty(type)) {
              forceVisibleOptions.add(type);
            }
          });
        } else if (HOLDINGS_NOTE == option) {
          var initial = action.getParameters().stream().filter(p -> HOLDINGS_NOTE_TYPE_ID_KEY.equals(p.getKey())).map(Parameter::getValue).findFirst();
          initial.ifPresent(id -> {
            var type = resolveAndGetItemTypeById(clazz, id);
            if (StringUtils.isNotEmpty(type)) {
              forceVisibleOptions.add(type);
            }
          });
        } else {
          // Default common case - the only this case should be processed in right approach
          forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(option, entityType));
        }
      });
    });
    log.info(format("Bulk Operation ID: %s, forced to visible fields: %s", bulkOperationId.toString(), String.join(",", forceVisibleOptions)));
    return forceVisibleOptions;
  }


  private String resolveAndGetItemTypeById(Class<? extends BulkOperationsEntity> clazz, String value) {
    if (clazz == HoldingsRecord.class) {
      return holdingsNoteTypeClient.getById(value).getName();
    } else if (clazz == Item.class) {
      return itemNoteTypeClient.getById(value).getName();
    } else {
      return StringUtils.EMPTY;
    }
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
