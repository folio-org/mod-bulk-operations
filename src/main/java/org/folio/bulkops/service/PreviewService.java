package org.folio.bulkops.service;

import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_HRID;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SOURCE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_STAFF_SUPPRESS;
import static org.folio.bulkops.domain.dto.ApproachType.MANUAL;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.UpdateOptionType.HOLDINGS_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.INSTANCE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ITEM_NOTE;
import static org.folio.bulkops.processor.HoldingsNotesUpdater.HOLDINGS_NOTE_TYPE_ID_KEY;
import static org.folio.bulkops.processor.InstanceNotesUpdaterFactory.INSTANCE_NOTE_TYPE_ID_KEY;
import static org.folio.bulkops.processor.ItemsNotesUpdater.ITEM_NOTE_TYPE_ID_KEY;
import static org.folio.bulkops.util.Constants.ELECTRONIC_ACCESS_HEADINGS;
import static org.folio.bulkops.util.Constants.FOLIO;
import static org.folio.bulkops.util.Constants.ITEM_DELIMITER_SPACED;
import static org.folio.bulkops.util.Utils.resolveEntityClass;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.folio.bulkops.client.InstanceNoteTypesClient;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.client.HoldingsNoteTypeClient;
import org.folio.bulkops.client.ItemNoteTypeClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.TenantNotePair;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.util.UnifiedTableHeaderBuilder;
import org.folio.bulkops.util.UpdateOptionTypeToFieldResolver;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
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
  private final InstanceNoteTypesClient instanceNoteTypesClient;
  private final MarcToUnifiedTableRowMapper marcToUnifiedTableRowMapper;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final BulkOperationService bulkOperationService;
  private final TenantTableUpdater tenantTableUpdater;
  private final Marc21ReferenceProvider referenceProvider;
  private final ObjectMapper objectMapper;

  private static final Pattern UUID_REGEX =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  public UnifiedTable getPreview(BulkOperation operation, BulkOperationStep step, int offset, int limit) {
    var entityType = operation.getEntityType();
    var clazz = resolveEntityClass(operation.getEntityType());
    return switch (step) {
      case UPLOAD -> buildPreviewFromCsvFile(operation.getLinkToMatchedRecordsCsvFile(), clazz, offset, limit, operation);
      case EDIT -> {
        var bulkOperationId = operation.getId();
        if (INSTANCE_MARC.equals(operation.getEntityType())) {
          yield buildCompositePreview(operation, offset, limit);
        } else {
          var rules = ruleService.getRules(bulkOperationId);
          var options = getChangedOptionsSet(bulkOperationId, entityType, rules, clazz);
          yield buildPreviewFromCsvFile(operation.getLinkToModifiedRecordsCsvFile(), clazz, offset, limit, options, operation);
        }
      }
      case COMMIT -> {
        if (MANUAL == operation.getApproach()) {
          yield buildPreviewFromCsvFile(operation.getLinkToCommittedRecordsCsvFile(), clazz, offset, limit, operation);
        } else {
          var bulkOperationId = operation.getId();
          if (INSTANCE_MARC.equals(operation.getEntityType())) {
            referenceProvider.updateMappingRules();
            var rules = ruleService.getMarcRules(bulkOperationId);
            var options = getChangedOptionsSet(rules);
            var marcTable = buildPreviewFromMarcFile(operation.getLinkToCommittedRecordsMarcFile(), clazz, offset, limit, options);
            enrichMarcWithAdministrativeData(marcTable, operation);
            yield marcTable;
          } else {
            var rules = ruleService.getRules(bulkOperationId);
            var options = getChangedOptionsSet(bulkOperationId, entityType, rules, clazz);
            yield buildPreviewFromCsvFile(operation.getLinkToCommittedRecordsCsvFile(), clazz, offset, limit, options, operation);
          }
        }
      }
    };
  }

  private UnifiedTable buildCompositePreview(BulkOperation bulkOperation, int offset, int limit) {
    var csvTable = buildPreviewFromCsvFile(bulkOperation.getLinkToMatchedRecordsCsvFile(), Instance.class, offset, limit, bulkOperation);
    if (isNotEmpty(bulkOperation.getLinkToModifiedRecordsMarcFile())) {
      referenceProvider.updateMappingRules();
      var rules = ruleService.getMarcRules(bulkOperation.getId());
      var changedOptionsSet = getChangedOptionsSet(rules);
      var compositeTable = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Instance.class);
      noteTableUpdater.extendTableWithInstanceNotesTypes(compositeTable, changedOptionsSet);
      var reader = new MarcStreamReader(remoteFileSystemClient.get(bulkOperation.getLinkToModifiedRecordsMarcFile()));
      var sourcePosition = getCellPositionByName(INSTANCE_SOURCE);
      var hridPosition = getCellPositionByName(INSTANCE_HRID);
      var administrativeNotesPosition = getCellPositionByName(INSTANCE_ADMINISTRATIVE_NOTE);
      var staffSuppressPosition = getCellPositionByName(INSTANCE_STAFF_SUPPRESS);
      var headers = compositeTable.getHeader().stream()
        .map(Cell::getValue)
        .toList();
      csvTable.getRows().forEach(csvRow -> {
        if (FOLIO.equals(csvRow.getRow().get(sourcePosition))) {
          compositeTable.addRowsItem(csvRow);
        } else {
          findMarcRecordByHrid(reader, csvRow.getRow().get(hridPosition)).ifPresent(marcRecord -> {
            var marcRow = new Row().row(marcToUnifiedTableRowMapper.processRecord(marcRecord, headers));
            marcRow.getRow().set(administrativeNotesPosition, csvRow.getRow().get(administrativeNotesPosition));
            marcRow.getRow().set(staffSuppressPosition, csvRow.getRow().get(staffSuppressPosition));
            compositeTable.addRowsItem(marcRow);
          });
        }
      });
      return compositeTable;
    }
    return csvTable;
  }

  private void enrichMarcWithAdministrativeData(UnifiedTable unifiedTable, BulkOperation bulkOperation) {
    var hridPosition = getCellPositionByName(INSTANCE_HRID);
    var administrativeNotesPosition = getCellPositionByName(INSTANCE_ADMINISTRATIVE_NOTE);
    var staffSuppressPosition = getCellPositionByName(INSTANCE_STAFF_SUPPRESS);
    try (var readerForMatchedJsonFile = remoteFileSystemClient.get(bulkOperation.getLinkToMatchedRecordsJsonFile())) {
      var iterator = objectMapper.readValues(new JsonFactory().createParser(readerForMatchedJsonFile), ExtendedInstance.class);
      unifiedTable.getRows().forEach(row -> findInstanceByHrid(iterator, row.getRow().get(hridPosition)).ifPresent(instance -> {
        row.getRow().set(staffSuppressPosition, isNull(instance.getStaffSuppress()) ? EMPTY : instance.getStaffSuppress().toString());
        row.getRow().set(administrativeNotesPosition, String.join(ITEM_DELIMITER_SPACED, instance.getAdministrativeNotes()));
      }));
    } catch (IOException e) {
      log.error("Failed to read json file", e);
    }
  }

  private Optional<Instance> findInstanceByHrid(MappingIterator<ExtendedInstance> iterator, String hrid) {
    while (iterator.hasNext()) {
      var instance = iterator.next().getEntity();
      if (hrid.equals(instance.getHrid())) {
        log.info("Instance hrid={}", hrid);
        return Optional.of(instance);
      }
    }
    log.error("Instance not found by hrid={}", hrid);
    return Optional.empty();
  }

  private Optional<Record> findMarcRecordByHrid(MarcReader reader, String hrid) {
    while (reader.hasNext()) {
      var marcRecord = reader.next();
      if (hrid.equals(marcRecord.getControlNumber())) {
        return Optional.of(marcRecord);
      }
    }
    log.error("MARC record was not found in file by hrid={}", hrid);
    return Optional.empty();
  }

  private int getCellPositionByName(String name) {
    return FieldUtils.getFieldsListWithAnnotation(Instance.class, CsvCustomBindByName.class).stream()
      .filter(field -> name.equals(field.getAnnotation(CsvCustomBindByName.class).column()))
      .map(field -> field.getAnnotation(CsvCustomBindByPosition.class).position())
      .findFirst().orElse(0);
  }

  private Set<String> getChangedOptionsSet(UUID bulkOperationId, EntityType entityType, BulkOperationRuleCollection rules, Class<? extends BulkOperationsEntity> clazz) {
    Set<String> forceVisibleOptions = new HashSet<>();
    var bulkOperation = bulkOperationService.getOperationById(bulkOperationId);
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

          if (EntityType.INSTANCE == entityType) {
            initial = CollectionUtils.isNotEmpty(action.getParameters()) ? action.getParameters().stream()
              .filter(p -> INSTANCE_NOTE_TYPE_ID_KEY.equals(p.getKey())).map(Parameter::getValue).findFirst() : Optional.empty();
          }

          if (initial.isPresent()) {
            var noteTypeName = getNoteTypeNameById(bulkOperation, initial.get());
            if (noteTypeName.isPresent()) {
              forceVisibleOptions.add(noteTypeName.get());
            } else {
              var type = resolveAndGetItemTypeById(clazz, initial.get());
              if (StringUtils.isNotEmpty(type)) {
                forceVisibleOptions.add(type);
              }
            }
            log.info("initial.isPresent() {},{}", folioExecutionContext.getTenantId(), initial.get());
          } else {
            forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(option, entityType));
          }

          var updated = action.getUpdated();
          if (UUID_REGEX.matcher(updated).matches()) {
            var noteTypeName = getNoteTypeNameById(bulkOperation, updated);
            if (noteTypeName.isPresent()) {
              forceVisibleOptions.add(noteTypeName.get());
            } else {
              var type = resolveAndGetItemTypeById(clazz, updated);
              if (StringUtils.isNotEmpty(type)) {
                forceVisibleOptions.add(type);
              }
            }
            log.info("UUID_REGEX.matcher(updated) {}, {}, {}", noteTypeName, updated, folioExecutionContext.getTenantId());
          } else {
            forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(UpdateOptionType.fromValue(updated), entityType));
          }
        } else if (action.getType() == UpdateActionType.DUPLICATE) {
          forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(option, entityType));
          forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(UpdateOptionType.fromValue(action.getUpdated()), entityType));
        } else if (ITEM_NOTE == option) {
          var initial = action.getParameters().stream().filter(p -> ITEM_NOTE_TYPE_ID_KEY.equals(p.getKey())).map(Parameter::getValue).findFirst();
          initial.ifPresent(id -> {
            var noteTypeName = getNoteTypeNameById(bulkOperation, id);
            if (noteTypeName.isPresent()) {
              forceVisibleOptions.add(noteTypeName.get());
            } else {
              var type = resolveAndGetItemTypeById(clazz, id);
              if (StringUtils.isNotEmpty(type)) {
                forceVisibleOptions.add(type);
              }
            }
          });
        } else if (HOLDINGS_NOTE == option) {
          var initial = action.getParameters().stream().filter(p -> HOLDINGS_NOTE_TYPE_ID_KEY.equals(p.getKey())).map(Parameter::getValue).findFirst();
          initial.ifPresent(id -> {
            var noteTypeName = getNoteTypeNameById(bulkOperation, id);
            if (noteTypeName.isPresent()) {
              forceVisibleOptions.add(noteTypeName.get());
            } else {
              var type = resolveAndGetItemTypeById(clazz, id);
              if (StringUtils.isNotEmpty(type)) {
                forceVisibleOptions.add(type);
              }
            }
          });
        } else if (INSTANCE_NOTE == option) {
          var initial = action.getParameters().stream().filter(p -> INSTANCE_NOTE_TYPE_ID_KEY.equals(p.getKey())).map(Parameter::getValue).findFirst();
          initial.ifPresent(id -> {
            log.info("else if (INSTANCE_NOTE == option)");
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

  private Set<String> getChangedOptionsSet(BulkOperationMarcRuleCollection rules) {
    Set<String> forceVisibleOptions = new HashSet<>();
    rules.getBulkOperationMarcRules()
      .stream().filter(rule -> referenceProvider.getMappedNoteTags().contains(rule.getTag()))
      .forEach(rule -> forceVisibleOptions.add(referenceProvider.getNoteTypeByTag(rule.getTag())));
    return forceVisibleOptions;
  }

  private String resolveAndGetItemTypeById(Class<? extends BulkOperationsEntity> clazz, String value) {
    if (clazz == HoldingsRecord.class) {
      return holdingsNoteTypeClient.getNoteTypeById(value).getName();
    } else if (clazz == Item.class) {
      return itemNoteTypeClient.getNoteTypeById(value).getName();
    } else if (clazz == Instance.class) {
      return instanceNoteTypesClient.getNoteTypeById(value).getName();
    } else {
      return StringUtils.EMPTY;
    }
  }

  private UnifiedTable buildPreviewFromCsvFile(String pathToFile, Class<? extends BulkOperationsEntity> clazz, int offset,
                                               int limit, Set<String> forceVisible, BulkOperation bulkOperation) {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(clazz, forceVisible);
    return populatePreview(pathToFile, clazz, offset, limit, table, forceVisible, bulkOperation);
  }

  private UnifiedTable buildPreviewFromCsvFile(String pathToFile, Class<? extends BulkOperationsEntity> clazz, int offset,
                                               int limit, BulkOperation bulkOperation) {
    var table =  UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(clazz);
    return populatePreview(pathToFile, clazz, offset, limit, table, emptySet(), bulkOperation);
  }

  private UnifiedTable buildPreviewFromMarcFile(String pathToFile, Class<? extends BulkOperationsEntity> clazz, int offset,
                                                int limit, Set<String> forceVisible) {
    var table =  UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(clazz);
    noteTableUpdater.extendTableWithInstanceNotesTypes(table, forceVisible);
    return isNotEmpty(pathToFile) ?
      populatePreviewFromMarc(pathToFile, offset, limit, table) :
      table.rows(Collections.emptyList());
  }

  private UnifiedTable populatePreview(String pathToFile, Class<? extends BulkOperationsEntity> clazz, int offset, int limit,
                                       UnifiedTable table, Set<String> forceVisible, BulkOperation bulkOperation) {
    var parser = new CSVParserBuilder().withEscapeChar('\0').build();

    try (Reader reader = new InputStreamReader(remoteFileSystemClient.get(pathToFile))) {
      var readerBuilder = new CSVReaderBuilder(reader)
        .withCSVParser(parser);
      CSVReader csvReader = readerBuilder.build();
        var recordsToSkip = offset + 1;
        csvReader.skip(recordsToSkip);
        String[] line;
        while ((line = csvReader.readNext()) != null && csvReader.getRecordsRead() <= limit + recordsToSkip) {
          var row = new org.folio.bulkops.domain.dto.Row();
          row.setRow(new ArrayList<>(Arrays.asList(line)));
          table.addRowsItem(row);
        }
      processNoteFields(table, clazz, forceVisible, bulkOperation);
      table.getRows().forEach(row -> {
        var rowData = row.getRow().stream()
          .map(s -> isEmpty(s) ? s : s.replace(ELECTRONIC_ACCESS_HEADINGS, EMPTY))
          .toList();
        row.setRow(SpecialCharacterEscaper.restore(rowData));
      });
      tenantTableUpdater.updateTenantInHeadersAndRows(table, clazz);
      return table;
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    tenantTableUpdater.updateTenantInHeadersAndRows(table, clazz);
    return table;
  }

  private UnifiedTable populatePreviewFromMarc(String pathToFile, int offset, int limit, UnifiedTable table) {
    var headers = table.getHeader().stream()
      .map(Cell::getValue)
      .toList();
    var reader = new MarcStreamReader(remoteFileSystemClient.get(pathToFile));
    var counter = 0;
    while (reader.hasNext() && counter < offset + limit) {
      counter++;
      var marcRecord = reader.next();
      if (counter >= offset) {
        table.addRowsItem(new Row().row(marcToUnifiedTableRowMapper.processRecord(marcRecord, headers)));
      }
    }
    return table;
  }

  private void processNoteFields(UnifiedTable table, Class<? extends BulkOperationsEntity> clazz, Set<String> forceVisible, BulkOperation bulkOperation) {
    if (clazz == Item.class) {
      noteTableUpdater.extendTableWithItemNotesTypes(table, forceVisible, bulkOperation);
    } else if (clazz == HoldingsRecord.class) {
      noteTableUpdater.extendTableWithHoldingsNotesTypes(table, forceVisible, bulkOperation);
    } else if (clazz == Instance.class) {
      noteTableUpdater.extendTableWithInstanceNotesTypes(table, forceVisible);
    }
  }

  private Optional<String> getNoteTypeNameById(BulkOperation bulkOperation, String noteTypeId) {
    return ofNullable(bulkOperation.getTenantNotePairs()).flatMap(pairs -> pairs.stream().filter(pair -> pair.getNoteTypeId().equals(noteTypeId))
      .map(TenantNotePair::getNoteTypeName).findFirst());
  }
}
