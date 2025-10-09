package org.folio.bulkops.service;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_CATALOGED_DATE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_CONTRIBUTORS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_FORMATS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_HRID;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_LANGUAGES;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_NATURE_OF_CONTENT;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_PREVIOUSLY_HELD;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_RESOURCE_TYPE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SET_FOR_DELETION;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SOURCE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_STAFF_SUPPRESS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_STATISTICAL_CODES;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_STATUS_TERM;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.domain.dto.ApproachType.MANUAL;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.UpdateOptionType.HOLDINGS_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.INSTANCE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ITEM_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SET_RECORDS_FOR_DELETE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STAFF_SUPPRESS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.processor.folio.HoldingsNotesUpdater.HOLDINGS_NOTE_TYPE_ID_KEY;
import static org.folio.bulkops.processor.folio.InstanceNotesUpdaterFactory.INSTANCE_NOTE_TYPE_ID_KEY;
import static org.folio.bulkops.processor.folio.ItemsNotesUpdater.ITEM_NOTE_TYPE_ID_KEY;
import static org.folio.bulkops.util.Constants.CLASSIFICATION_HEADINGS;
import static org.folio.bulkops.util.Constants.ELECTRONIC_ACCESS_HEADINGS;
import static org.folio.bulkops.util.Constants.FOLIO;
import static org.folio.bulkops.util.Constants.PUBLICATION_HEADINGS;
import static org.folio.bulkops.util.Constants.SUBJECT_HEADINGS;
import static org.folio.bulkops.util.Utils.resolveEntityClass;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.folio.bulkops.client.HoldingsNoteTypeClient;
import org.folio.bulkops.client.InstanceNoteTypesClient;
import org.folio.bulkops.client.ItemNoteTypeClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.Row;
import org.folio.bulkops.domain.dto.RuleDetails;
import org.folio.bulkops.domain.dto.TenantNotePair;
import org.folio.bulkops.domain.dto.UnifiedTable;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;
import org.folio.bulkops.util.CsvHelper;
import org.folio.bulkops.util.UnifiedTableHeaderBuilder;
import org.folio.bulkops.util.UpdateOptionTypeToFieldResolver;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.springframework.stereotype.Service;

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

  private static final Pattern UUID_REGEX =
          Pattern.compile(
                  "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  private final Marc21ReferenceProvider marc21ReferenceProvider;

  public UnifiedTable getPreview(BulkOperation operation, BulkOperationStep step, int offset,
                                 int limit) {
    var clazz = resolveEntityClass(operation.getEntityType());
    return switch (step) {
      case UPLOAD -> buildPreviewFromCsvFile(operation.getLinkToMatchedRecordsCsvFile(), clazz,
              offset, limit, operation);
      case EDIT -> {
        if (INSTANCE_MARC == operation.getEntityType()) {
          if (isFolioInstanceEditPreview(operation)) {
            yield getPreviewFromCsvWithChangedOptions(operation,
                    operation.getLinkToModifiedRecordsCsvFile(), offset, limit);
          } else if (isMarcInstanceEditPreview(operation)) {
            yield buildCompositePreview(operation, offset, limit,
                    operation.getLinkToMatchedRecordsCsvFile(),
                    operation.getLinkToModifiedRecordsMarcFile());
          } else {
            yield buildCompositePreview(operation, offset, limit,
                    operation.getLinkToModifiedRecordsCsvFile(),
                    operation.getLinkToModifiedRecordsMarcFile());
          }
        } else {
          yield getPreviewFromCsvWithChangedOptions(operation,
                  operation.getLinkToModifiedRecordsCsvFile(), offset, limit);
        }
      }
      case COMMIT -> {
        if (MANUAL == operation.getApproach()) {
          yield buildPreviewFromCsvFile(operation.getLinkToCommittedRecordsCsvFile(),
                  clazz, offset, limit, operation);
        } else {
          if (INSTANCE_MARC == operation.getEntityType()) {
            if (isFolioInstanceCommitPreview(operation)) {
              yield getPreviewFromCsvWithChangedOptions(operation,
                      operation.getLinkToCommittedRecordsCsvFile(), offset, limit);
            } else if (isMarcInstanceCommitPreview(operation)) {
              yield buildCompositePreview(operation, offset, limit,
                      operation.getLinkToMatchedRecordsCsvFile(),
                      operation.getLinkToCommittedRecordsMarcFile());
            } else {
              yield buildCompositePreview(operation, offset, limit,
                      operation.getLinkToCommittedRecordsCsvFile(),
                      operation.getLinkToCommittedRecordsMarcFile());
            }
          } else {
            yield getPreviewFromCsvWithChangedOptions(operation,
                    operation.getLinkToCommittedRecordsCsvFile(), offset, limit);
          }
        }
      }
    };
  }

  private UnifiedTable getPreviewFromCsvWithChangedOptions(BulkOperation operation,
                                                           String linkToCsvFile,
                                                           int offset, int limit) {
    var clazz = resolveEntityClass(operation.getEntityType());
    var rules = ruleService.getRules(operation.getId());
    var options = getChangedOptionsSet(operation.getId(), operation.getEntityType(), rules, clazz);
    return buildPreviewFromCsvFile(linkToCsvFile, clazz, offset, limit, options, operation);
  }

  private boolean isFolioInstanceEditPreview(BulkOperation operation) {
    return StringUtils.isNotEmpty(operation.getLinkToModifiedRecordsCsvFile())
            && StringUtils.isEmpty(operation.getLinkToModifiedRecordsMarcFile());
  }

  private boolean isMarcInstanceEditPreview(BulkOperation operation) {
    return StringUtils.isNotEmpty(operation.getLinkToModifiedRecordsMarcFile())
            && StringUtils.isEmpty(operation.getLinkToModifiedRecordsCsvFile());
  }

  private boolean isFolioInstanceCommitPreview(BulkOperation operation) {
    return StringUtils.isNotEmpty(operation.getLinkToCommittedRecordsCsvFile())
            && StringUtils.isEmpty(operation.getLinkToCommittedRecordsMarcFile());
  }

  private boolean isMarcInstanceCommitPreview(BulkOperation operation) {
    return StringUtils.isNotEmpty(operation.getLinkToCommittedRecordsMarcFile())
            && StringUtils.isEmpty(operation.getLinkToCommittedRecordsCsvFile());
  }

  private UnifiedTable buildCompositePreview(BulkOperation bulkOperation, int offset, int limit,
                                             String linkToCsvFile, String linkToMarcFile) {
    var csvTable = buildPreviewFromCsvFile(linkToCsvFile, resolveEntityClass(
            bulkOperation.getEntityType()), offset, limit, bulkOperation);
    if (isNotEmpty(linkToMarcFile)) {
      referenceProvider.updateMappingRules();
      var marcRules = ruleService.getMarcRules(bulkOperation.getId());
      var changedOptionsSet = marc21ReferenceProvider.getChangedOptionsSet(marcRules);
      var compositeTable = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(Instance.class);
      forceVisibleMarcFields(compositeTable, changedOptionsSet);
      noteTableUpdater.extendTableWithInstanceNotesTypes(compositeTable, changedOptionsSet);
      var sourcePosition = getCellPositionByName(INSTANCE_SOURCE);
      var hridPosition = getCellPositionByName(INSTANCE_HRID);
      var headers = compositeTable.getHeader().stream()
              .map(Cell::getValue)
              .toList();
      var hrids = csvTable.getRows().stream()
              .map(row -> row.getRow().get(hridPosition))
              .toList();
      var marcRecords = findMarcRecordsByHrids(linkToMarcFile, hrids);
      csvTable.getRows().forEach(csvRow -> {
        if (FOLIO.equals(csvRow.getRow().get(sourcePosition))) {
          compositeTable.addRowsItem(csvRow);
        } else {
          var hrid = csvRow.getRow().get(hridPosition);
          if (marcRecords.containsKey(hrid)) {
            var marcRow = new Row().row(marcToUnifiedTableRowMapper.processRecord(
                    marcRecords.get(hrid), headers, false));
            enrichRowWithAdministrativeData(marcRow, csvRow);
            compositeTable.addRowsItem(marcRow);
          }
        }
      });
      forceVisibleAdministrativeDataHeaders(compositeTable, bulkOperation);
      return compositeTable;
    }
    forceVisibleAdministrativeDataHeaders(csvTable, bulkOperation);
    return csvTable;
  }

  private void forceVisibleAdministrativeDataHeaders(UnifiedTable unifiedTable,
                                                     BulkOperation bulkOperation) {
    var rules = ruleService.getRules(bulkOperation.getId());
    var forceVisibleFieldNames = rules.getBulkOperationRules().stream()
            .map(BulkOperationRule::getRuleDetails)
            .map(RuleDetails::getOption)
            .map(option -> UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(
                    option, INSTANCE))
            .collect(Collectors.toSet());
    unifiedTable.getHeader().forEach(header -> {
      if (forceVisibleFieldNames.contains(header.getValue())) {
        header.setForceVisible(true);
      }
    });
    if (isSetForDeletionForceVisible(unifiedTable)) {
      unifiedTable.getHeader().stream().filter(header -> header.getValue().equals("Staff suppress")
                      || header.getValue().equals("Suppress from discovery"))
              .forEach(header -> header.setForceVisible(true));
    }
  }

  private boolean isSetForDeletionForceVisible(UnifiedTable unifiedTable) {
    return unifiedTable.getHeader().stream()
            .anyMatch(header -> header.getValue().equals("Set for deletion")
                    && header.getForceVisible());
  }

  private void forceVisibleMarcFields(UnifiedTable unifiedTable, Set<String> changedOptionsSet) {
    unifiedTable.getHeader().forEach(cell -> {
      if (changedOptionsSet.contains(cell.getValue())) {
        cell.setForceVisible(true);
      }
    });
  }

  private void enrichRowWithAdministrativeData(Row marcRow, Row csvRow) {
    var positions = getAdministrativeDataFieldPositions();
    marcRow.getRow().set(positions.get(INSTANCE_STAFF_SUPPRESS), csvRow.getRow()
            .get(positions.get(INSTANCE_STAFF_SUPPRESS)));
    marcRow.getRow().set(positions.get(INSTANCE_ADMINISTRATIVE_NOTE), csvRow.getRow()
            .get(positions.get(INSTANCE_ADMINISTRATIVE_NOTE)));
    marcRow.getRow().set(positions.get(INSTANCE_SUPPRESS_FROM_DISCOVERY), csvRow.getRow()
            .get(positions.get(INSTANCE_SUPPRESS_FROM_DISCOVERY)));
    marcRow.getRow().set(positions.get(INSTANCE_PREVIOUSLY_HELD), csvRow.getRow()
            .get(positions.get(INSTANCE_PREVIOUSLY_HELD)));
    marcRow.getRow().set(positions.get(INSTANCE_STATUS_TERM), csvRow.getRow()
            .get(positions.get(INSTANCE_STATUS_TERM)));
    marcRow.getRow().set(positions.get(INSTANCE_NATURE_OF_CONTENT), csvRow.getRow()
            .get(positions.get(INSTANCE_NATURE_OF_CONTENT)));
    marcRow.getRow().set(positions.get(INSTANCE_CATALOGED_DATE), csvRow.getRow()
            .get(positions.get(INSTANCE_CATALOGED_DATE)));
    marcRow.getRow().set(positions.get(INSTANCE_STATISTICAL_CODES), csvRow.getRow()
            .get(positions.get(INSTANCE_STATISTICAL_CODES)));
    marcRow.getRow().set(positions.get(INSTANCE_SET_FOR_DELETION), csvRow.getRow()
            .get(positions.get(INSTANCE_SET_FOR_DELETION)));
  }

  private Map<String, Integer> getAdministrativeDataFieldPositions() {
    var positions = new HashMap<String, Integer>();
    List.of(INSTANCE_STAFF_SUPPRESS,
        INSTANCE_ADMINISTRATIVE_NOTE,
        INSTANCE_SUPPRESS_FROM_DISCOVERY,
        INSTANCE_PREVIOUSLY_HELD,
        INSTANCE_CATALOGED_DATE,
        INSTANCE_STATUS_TERM,
        INSTANCE_NATURE_OF_CONTENT,
        INSTANCE_FORMATS,
        INSTANCE_CONTRIBUTORS,
        INSTANCE_RESOURCE_TYPE,
        INSTANCE_LANGUAGES,
        INSTANCE_STATISTICAL_CODES,
        INSTANCE_SET_FOR_DELETION)
            .forEach(name -> positions.put(name, getCellPositionByName(name)));
    return positions;
  }

  private Map<String, Record> findMarcRecordsByHrids(String linkToMarcFile, List<String> hrids) {
    var map = new HashMap<String, Record>();
    var list = new ArrayList<>(hrids);
    try (var is = remoteFileSystemClient.get(linkToMarcFile)) {
      var reader = new MarcStreamReader(is);
      while (reader.hasNext() && !list.isEmpty()) {
        var marcRecord = reader.next();
        var hrid = marcRecord.getControlNumber();
        if (list.contains(hrid)) {
          map.put(hrid, marcRecord);
          list.remove(hrid);
        }
      }
    } catch (IOException e) {
      log.error("Failed to read file {}", linkToMarcFile, e);
    }
    return map;
  }

  private int getCellPositionByName(String name) {
    return FieldUtils.getFieldsListWithAnnotation(Instance.class,
                    CsvCustomBindByName.class).stream()
      .filter(field -> name.equals(field.getAnnotation(CsvCustomBindByName.class).column()))
      .map(field -> field.getAnnotation(CsvCustomBindByPosition.class).position())
      .findFirst().orElseThrow(() -> new IllegalArgumentException(
              "Field position was not found by name=" + name));
  }

  private Set<String> getChangedOptionsSet(UUID bulkOperationId, EntityType entityType,
                                           BulkOperationRuleCollection rules,
                                           Class<? extends BulkOperationsEntity> clazz) {
    Set<String> forceVisibleOptions = new HashSet<>();
    var bulkOperation = bulkOperationService.getOperationById(bulkOperationId);
    rules.getBulkOperationRules().forEach(rule -> {
      var option = rule.getRuleDetails().getOption();
      if (SET_RECORDS_FOR_DELETE == option && entityType == INSTANCE) {
        forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(
                STAFF_SUPPRESS, entityType));
        forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(
                SUPPRESS_FROM_DISCOVERY, entityType));
      }
      rule.getRuleDetails().getActions().forEach(action -> {

        if (action.getType() == UpdateActionType.CHANGE_TYPE) {

          Optional<String> initial = getInitialNoteTypeId(entityType, action);

          if (initial.isPresent()) {
            processInitialNoteType(bulkOperation, forceVisibleOptions, initial.get(), clazz);
          } else {
            forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(
                    option, entityType));
          }

          var updated = action.getUpdated();
          if (UUID_REGEX.matcher(updated).matches()) {
            processUpdatedNoteType(bulkOperation, forceVisibleOptions, updated, clazz);
          } else {
            forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(
                    UpdateOptionType.fromValue(updated), entityType));
          }
        } else if (action.getType() == UpdateActionType.DUPLICATE) {
          forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(
                  option, entityType));
          forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(
                  UpdateOptionType.fromValue(action.getUpdated()), entityType));
        } else if (ITEM_NOTE == option) {
          var initial = action.getParameters().stream().filter(
                  p -> ITEM_NOTE_TYPE_ID_KEY.equals(p.getKey())).map(Parameter::getValue)
                  .findFirst();
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
          processInitialHoldingsNote(bulkOperation, forceVisibleOptions, action, clazz);
        } else if (INSTANCE_NOTE == option) {
          processInitialInstanceNote(forceVisibleOptions, action, clazz);
        } else {
          // Default common case - the only this case should be processed in right approach
          forceVisibleOptions.add(UpdateOptionTypeToFieldResolver.getFieldByUpdateOptionType(
                  option, entityType));
        }
      });
    });
    log.info("Bulk Operation ID: {}, forced to visible fields: {}", bulkOperationId.toString(),
            String.join(",", forceVisibleOptions));
    return forceVisibleOptions;
  }

  private String resolveAndGetItemTypeById(Class<? extends BulkOperationsEntity> clazz,
                                           String value) {
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

  private UnifiedTable buildPreviewFromCsvFile(String pathToFile,
                                               Class<? extends BulkOperationsEntity> clazz,
                                               int offset,
                                               int limit, Set<String> forceVisible,
                                               BulkOperation bulkOperation) {
    var table = UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(clazz, forceVisible);
    return populatePreview(pathToFile, clazz, offset, limit, table, forceVisible, bulkOperation);
  }

  private UnifiedTable buildPreviewFromCsvFile(String pathToFile,
                                               Class<? extends BulkOperationsEntity> clazz,
                                               int offset, int limit, BulkOperation bulkOperation) {
    var table =  UnifiedTableHeaderBuilder.getEmptyTableWithHeaders(clazz);
    return populatePreview(pathToFile, clazz, offset, limit, table, emptySet(), bulkOperation);
  }

  private UnifiedTable populatePreview(String pathToFile,
                                       Class<? extends BulkOperationsEntity> clazz,
                                       int offset, int limit,
                                       UnifiedTable table, Set<String> forceVisible,
                                       BulkOperation bulkOperation) {
    try (Reader reader = new InputStreamReader(remoteFileSystemClient.get(pathToFile))) {
      var readerBuilder = new CSVReaderBuilder(reader)
              .withCSVParser(CsvHelper.getCsvParser());
      CSVReader csvReader = readerBuilder.build();
      var recordsToSkip = offset + 1;
      csvReader.skip(recordsToSkip);
      String[] line;
      while ((line = csvReader.readNext()) != null && csvReader.getRecordsRead() <= limit
              + recordsToSkip) {
        var row = new org.folio.bulkops.domain.dto.Row();
        row.setRow(new ArrayList<>(Arrays.asList(line)));
        table.addRowsItem(row);
      }
      processNoteFields(table, clazz, forceVisible, bulkOperation);
      table.getRows().forEach(row -> {
        var rowData = removeSubColumnsAndGetRowForPreview(row);
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

  private List<String> removeSubColumnsAndGetRowForPreview(Row row) {
    return row.getRow().stream()
        .map(s -> isEmpty(s) ? s : s.replace(ELECTRONIC_ACCESS_HEADINGS, EMPTY))
        .map(s -> isEmpty(s) ? s : s.replace(SUBJECT_HEADINGS, EMPTY))
        .map(s -> isEmpty(s) ? s : s.replace(CLASSIFICATION_HEADINGS, EMPTY))
        .map(s -> isEmpty(s) ? s : s.replace(PUBLICATION_HEADINGS, EMPTY))
        .toList();
  }

  private void processNoteFields(UnifiedTable table, Class<? extends BulkOperationsEntity> clazz,
                                 Set<String> forceVisible, BulkOperation bulkOperation) {
    if (clazz == Item.class) {
      noteTableUpdater.extendTableWithItemNotesTypes(table, forceVisible, bulkOperation);
    } else if (clazz == HoldingsRecord.class) {
      noteTableUpdater.extendTableWithHoldingsNotesTypes(table, forceVisible, bulkOperation);
    } else if (clazz == Instance.class) {
      noteTableUpdater.extendTableWithInstanceNotesTypes(table, forceVisible);
    }
  }

  private Optional<String> getNoteTypeNameById(BulkOperation bulkOperation, String noteTypeId) {
    return ofNullable(bulkOperation.getTenantNotePairs()).flatMap(
            pairs -> pairs.stream().filter(
                    pair -> pair.getNoteTypeId().equals(noteTypeId))
      .map(TenantNotePair::getNoteTypeName).findFirst());
  }

  private Optional<String> getInitialNoteTypeId(EntityType entityType,
                                                org.folio.bulkops.domain.dto.Action action) {
    Optional<String> initial = Optional.empty();
    if (EntityType.ITEM == entityType) {
      initial = CollectionUtils.isNotEmpty(action.getParameters())
              ? action.getParameters().stream()
              .filter(p -> ITEM_NOTE_TYPE_ID_KEY.equals(p.getKey()))
              .map(Parameter::getValue).findFirst() : Optional.empty();
    }

    if (EntityType.HOLDINGS_RECORD == entityType) {
      initial = CollectionUtils.isNotEmpty(action.getParameters())
              ? action.getParameters().stream()
              .filter(p -> HOLDINGS_NOTE_TYPE_ID_KEY.equals(p.getKey()))
              .map(Parameter::getValue).findFirst() : Optional.empty();
    }

    if (EntityType.INSTANCE == entityType) {
      initial = CollectionUtils.isNotEmpty(action.getParameters())
              ? action.getParameters().stream()
              .filter(p -> INSTANCE_NOTE_TYPE_ID_KEY.equals(p.getKey()))
              .map(Parameter::getValue).findFirst() : Optional.empty();
    }
    return initial;
  }

  private void processInitialNoteType(BulkOperation bulkOperation, Set<String> forceVisibleOptions,
                                      String initial,
                                      Class<? extends BulkOperationsEntity> clazz) {
    var noteTypeName = getNoteTypeNameById(bulkOperation, initial);
    if (noteTypeName.isPresent()) {
      forceVisibleOptions.add(noteTypeName.get());
    } else {
      var type = resolveAndGetItemTypeById(clazz, initial);
      if (StringUtils.isNotEmpty(type)) {
        forceVisibleOptions.add(type);
      }
    }
    log.info("initial.isPresent() {},{}", folioExecutionContext.getTenantId(),
            initial);
  }

  private void processUpdatedNoteType(BulkOperation bulkOperation, Set<String> forceVisibleOptions,
                                     String updated,
                                     Class<? extends BulkOperationsEntity> clazz) {
    var noteTypeName = getNoteTypeNameById(bulkOperation, updated);
    if (noteTypeName.isPresent()) {
      forceVisibleOptions.add(noteTypeName.get());
    } else {
      var type = resolveAndGetItemTypeById(clazz, updated);
      if (StringUtils.isNotEmpty(type)) {
        forceVisibleOptions.add(type);
      }
    }
    log.info("UUID_REGEX.matcher(updated) {}, {}, {}", noteTypeName, updated,
            folioExecutionContext.getTenantId());
  }

  private void processInitialHoldingsNote(BulkOperation bulkOperation,
                                          Set<String> forceVisibleOptions,
                                          org.folio.bulkops.domain.dto.Action action,
                                          Class<? extends BulkOperationsEntity> clazz) {
    var initial = action.getParameters().stream().filter(
                    p -> HOLDINGS_NOTE_TYPE_ID_KEY.equals(p.getKey())).map(Parameter::getValue)
            .findFirst();
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
  }

  private void processInitialInstanceNote(Set<String> forceVisibleOptions,
                                          org.folio.bulkops.domain.dto.Action action,
                                          Class<? extends BulkOperationsEntity> clazz) {
    var initial = action.getParameters().stream().filter(
                    p -> INSTANCE_NOTE_TYPE_ID_KEY.equals(p.getKey())).map(Parameter::getValue)
            .findFirst();
    initial.ifPresent(id -> {
      var type = resolveAndGetItemTypeById(clazz, id);
      if (StringUtils.isNotEmpty(type)) {
        forceVisibleOptions.add(type);
      }
    });
  }
}
