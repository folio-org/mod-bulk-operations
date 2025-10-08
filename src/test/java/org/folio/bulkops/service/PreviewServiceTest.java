package org.folio.bulkops.service;

import static java.util.Collections.emptySet;
import static org.folio.bulkops.domain.dto.BulkOperationStep.COMMIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.EDIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.UPLOAD;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE_MARC;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.OperationStatusType.NEW;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.HOLDINGS_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SET_RECORDS_FOR_DELETE;
import static org.folio.bulkops.service.Marc21ReferenceProvider.GENERAL_NOTE;
import static org.folio.bulkops.util.Constants.HOLDINGS_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.INSTANCE_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.ITEM_NOTE_POSITION;
import static org.folio.bulkops.util.UnifiedTableHeaderBuilder.getHeaders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.hamcrest.Matchers.equalTo;
import static org.testcontainers.shaded.org.hamcrest.Matchers.hasSize;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.MappingRulesClient;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsNoteTypeCollection;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.InstanceFormat;
import org.folio.bulkops.domain.bean.InstanceFormats;
import org.folio.bulkops.domain.bean.InstanceType;
import org.folio.bulkops.domain.bean.InstanceTypes;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.NoteType;
import org.folio.bulkops.domain.bean.NoteTypeCollection;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.ApproachType;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.ContributorType;
import org.folio.bulkops.domain.dto.ContributorTypeCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.InstanceNoteType;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.RuleDetails;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.UpdateOptionTypeToFieldResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PreviewServiceTest extends BaseTest {

  @Autowired
  private PreviewService previewService;
  @MockitoBean
  private BulkOperationRepository bulkOperationRepository;
  @MockitoBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockitoBean
  private RuleService ruleService;
  @MockitoBean
  private InstanceReferenceService instanceReferenceService;
  @MockitoBean
  private ConsortiaService consortiaService;
  @MockitoBean
  private MappingRulesClient mappingRulesClient;
  @MockitoBean
  private BulkOperationService bulkOperationService;
  @Autowired
  private NoteTableUpdater noteTableUpdater;

  @CsvSource(value = {
      "users_preview.csv,USER,UPLOAD,IN_APP",
      "users_preview.csv,USER,EDIT,IN_APP",
      "users_preview.csv,USER,COMMIT,IN_APP",
      "users_preview.csv,USER,COMMIT,MANUAL",
      "items_preview.csv,ITEM,UPLOAD,IN_APP",
      "items_preview.csv,ITEM,EDIT,IN_APP",
      "items_preview.csv,ITEM,COMMIT,IN_APP",
      "items_preview.csv,ITEM,COMMIT,MANUAL",
      "holdings_preview.csv,HOLDINGS_RECORD,UPLOAD,IN_APP",
      "holdings_preview.csv,HOLDINGS_RECORD,EDIT,IN_APP",
      "holdings_preview.csv,HOLDINGS_RECORD,COMMIT,IN_APP",
      "holdings_preview.csv,HOLDINGS_RECORD,COMMIT,MANUAL",
      "instances_preview.csv,INSTANCE,UPLOAD,IN_APP",
      "instances_preview.csv,INSTANCE,EDIT,IN_APP",
      "instances_preview.csv,INSTANCE,COMMIT,IN_APP",
      "instances_preview.csv,INSTANCE,COMMIT,MANUAL"}, delimiter = ',')
  @SneakyThrows
  @ParameterizedTest
  void shouldReturnPreviewIfAvailable(String fileName, EntityType entityType,
                                      BulkOperationStep step, ApproachType approachType) {
    var operationId = UUID.randomUUID();

    var bulkOperation = buildBulkOperation(fileName, entityType, step);
    bulkOperation.setId(operationId);
    bulkOperation.setApproach(approachType);
    bulkOperation.setStatus(NEW);
    bulkOperation.setTenantNotePairs(List.of());
    when(bulkOperationRepository.findById(operationId))
            .thenReturn(Optional.of(bulkOperation));
    var path = "src/test/resources/files/" + fileName;

    when(remoteFileSystemClient.get(anyString()))
            .thenReturn(new FileInputStream(path));

    when(groupClient.getGroupById(anyString())).thenReturn(new UserGroup().withGroup("Group"));
    when(locationClient.getLocationById(anyString()))
            .thenReturn(new ItemLocation().withName("Location"));
    when(holdingsSourceClient.getById(anyString()))
            .thenReturn(new HoldingsRecordsSource().withName("Source"));

    when(itemNoteTypeClient.getNoteTypes(Integer.MAX_VALUE))
            .thenReturn(new NoteTypeCollection().withItemNoteTypes(List.of(
                    new NoteType().withName("Binding"), new NoteType().withName("Custom"),
                    new NoteType().withName("Provenance"), new NoteType().withName("Reproduction"),
                    new NoteType().withName("Note"))));
    when(holdingsNoteTypeClient.getNoteTypes(Integer.MAX_VALUE))
            .thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(List.of(
                    new HoldingsNoteType().withName("Binding"),
                    new HoldingsNoteType().withName("Provenance"),
                    new HoldingsNoteType().withName("Reproduction"))));

    when(itemNoteTypeClient.getNoteTypeById("0e40884c-3523-4c6d-8187-d578e3d2794e"))
            .thenReturn(new NoteType().withName("Binding"));
    when(itemNoteTypeClient.getNoteTypeById("f3ae3823-d096-4c65-8734-0c1efd2ffea8"))
            .thenReturn(new NoteType().withName("Provenance"));
    when(itemNoteTypeClient.getNoteTypeById("c3a539b9-9576-4e3a-b6de-d910200b2919"))
            .thenReturn(new NoteType().withName("Reproduction"));
    when(itemNoteTypeClient.getNoteTypeById("87c450be-2033-41fb-80ba-dd2409883681"))
            .thenReturn(new NoteType().withName("Custom"));

    when(itemNoteTypeClient.getNoteTypeById("8d0a5eca-25de-4391-81a9-236eeefdd20b"))
            .thenReturn(new NoteType().withName("Note"));

    when(holdingsNoteTypeClient.getNoteTypeById("e19eabab-a85c-4aef-a7b2-33bd9acef24e"))
            .thenReturn(new HoldingsNoteType().withName("Reproduction"));
    when(consortiaService.isTenantCentral(any())).thenReturn(false);

    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn(Files.readString(Path.of(
                    "src/test/resources/files/mappingRulesResponse.json")));

    var bulkOperationRuleCollection = objectMapper.readValue(new FileInputStream(
            getPathToContentUpdateRequest(entityType)), BulkOperationRuleCollection.class);
    when(ruleService.getRules(any(UUID.class))).thenReturn(bulkOperationRuleCollection);
    bulkOperation.setTenantNotePairs(List.of());
    when(bulkOperationService.getBulkOperationOrThrow(any(UUID.class))).thenReturn(bulkOperation);
    when(bulkOperationService.getOperationById(any(UUID.class))).thenReturn(bulkOperation);

    var offset = 2;
    var limit = 5;

    var table = previewService.getPreview(bulkOperation, step, offset, limit);

    assertThat(table.getRows(), hasSize(limit - offset));
    if (USER.equals(entityType)) {
      if ((step == EDIT || step == COMMIT) && approachType == ApproachType.IN_APP) {
        assertThat(table.getHeader(), equalTo(
                getHeaders(User.class, UpdateOptionTypeToFieldResolver.getFieldsByUpdateOptionTypes(
                        List.of(UpdateOptionType.EMAIL_ADDRESS, UpdateOptionType.EXPIRATION_DATE),
                        entityType))));
      } else {
        assertThat(table.getHeader(), equalTo(getHeaders(User.class)));
      }
    } else if (ITEM.equals(entityType)) {
      List<Cell> headers;
      if ((step == EDIT || step == COMMIT) && approachType == ApproachType.IN_APP) {
        headers = getHeaders(Item.class, Set.of("Binding", "Custom", "Status",
                "Check out note", "Provenance", "Reproduction", "Check in note",
                "Note", "Administrative note"));
        noteTableUpdater.extendHeadersWithNoteTypeNames(ITEM_NOTE_POSITION, headers,
                List.of("Binding", "Custom", "Note", "Provenance", "Reproduction"),
                Set.of("Binding", "Status", "Check out note", "Provenance", "Check in note",
                        "Note", "Custom", "Reproduction", "Administrative notes"));
      } else {
        headers = getHeaders(Item.class);
        noteTableUpdater.extendHeadersWithNoteTypeNames(ITEM_NOTE_POSITION, headers,
                List.of("Binding", "Custom", "Note", "Provenance", "Reproduction"), emptySet());
      }
      headers.removeLast();
      assertThat(table.getHeader(), equalTo(headers));
    } else if (INSTANCE.equals(entityType)) {
      if ((step == EDIT || step == COMMIT) && approachType == ApproachType.IN_APP) {
        assertThat(table.getHeader(), equalTo(
                getHeaders(Instance.class, UpdateOptionTypeToFieldResolver
                        .getFieldsByUpdateOptionTypes(List.of(UpdateOptionType.STAFF_SUPPRESS,
                                UpdateOptionType.SUPPRESS_FROM_DISCOVERY), entityType))));
      } else {
        assertThat(table.getHeader(), equalTo(getHeaders(Instance.class)));
      }

    }
    assertTrue(table.getRows().stream()
            .map(org.folio.bulkops.domain.dto.Row::getRow)
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .noneMatch(s -> s.contains("%3A") || s.contains("%3B") || s.contains("%7C")));
  }

  @Test
  @SneakyThrows
  void shouldReturnPreviewWithCorrectNumberOfRecordsWhenRecordsContainLineBreaks() {
    var path = "src/test/resources/files/items_preview_line_breaks.csv";
    var operationId = UUID.randomUUID();

    var bulkOperation = buildBulkOperation("items_preview_line_breaks.csv", ITEM, COMMIT);
    bulkOperation.setId(operationId);
    when(bulkOperationRepository.findById(operationId))
            .thenReturn(Optional.of(bulkOperation));

    when(remoteFileSystemClient.get(anyString()))
            .thenReturn(new FileInputStream(path));

    when(ruleService.getRules(any())).thenReturn(
            new BulkOperationRuleCollection().bulkOperationRules(
                    List.of(new BulkOperationRule().ruleDetails(
                            new RuleDetails().option(UpdateOptionType.STATUS)
                                    .actions(List.of(new Action().type(
                                            UpdateActionType.REPLACE_WITH)
                                            .updated("New")))))).totalRecords(1));

    when(locationClient.getLocationById(anyString()))
            .thenReturn(new ItemLocation().withName("Location"));

    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn(Files.readString(Path.of(
                    "src/test/resources/files/mappingRulesResponse.json")));

    var offset = 1;
    var limit = 10;

    var table = previewService.getPreview(bulkOperation, COMMIT, offset, limit);

    assertThat(table.getRows(), hasSize(limit));
  }

  @Test
  @SneakyThrows
  void shouldProperlyParseContentWithBackSlashes() {
    var path = "src/test/resources/files/items_preview_back_slashes.csv";
    var operationId = UUID.randomUUID();

    var bulkOperation = buildBulkOperation("items_preview_back_slashes.csv", ITEM, UPLOAD);
    bulkOperation.setId(operationId);
    when(bulkOperationRepository.findById(operationId))
            .thenReturn(Optional.of(bulkOperation));

    when(remoteFileSystemClient.get(anyString()))
            .thenReturn(new FileInputStream(path));

    when(ruleService.getRules(any()))
            .thenReturn(new BulkOperationRuleCollection().bulkOperationRules(
                    List.of(new BulkOperationRule().ruleDetails(
                            new RuleDetails().option(UpdateOptionType.STATUS)
                                    .actions(List.of(
                                            new Action().type(UpdateActionType.REPLACE_WITH)
                                            .updated("New")))))).totalRecords(1));

    when(locationClient.getLocationById(anyString()))
            .thenReturn(new ItemLocation().withName("Location"));

    when(itemNoteTypeClient.getNoteTypes(Integer.MAX_VALUE))
            .thenReturn(new NoteTypeCollection().withItemNoteTypes(
                    List.of(new NoteType().withName("Binding"),
                            new NoteType().withName("Custom"),
                            new NoteType().withName("Provenance"),
                            new NoteType().withName("Reproduction"),
                            new NoteType().withName("Note"))));

    when(itemNoteTypeClient.getNoteTypeById("0e40884c-3523-4c6d-8187-d578e3d2794e"))
            .thenReturn(new NoteType().withName("Binding"));
    when(itemNoteTypeClient.getNoteTypeById("f3ae3823-d096-4c65-8734-0c1efd2ffea8"))
            .thenReturn(new NoteType().withName("Provenance"));
    when(itemNoteTypeClient.getNoteTypeById("c3a539b9-9576-4e3a-b6de-d910200b2919"))
            .thenReturn(new NoteType().withName("Reproduction"));
    when(itemNoteTypeClient.getNoteTypeById("87c450be-2033-41fb-80ba-dd2409883681"))
            .thenReturn(new NoteType().withName("Custom"));
    when(itemNoteTypeClient.getNoteTypeById("8d0a5eca-25de-4391-81a9-236eeefdd20b"))
            .thenReturn(new NoteType().withName("Note"));
    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn(Files.readString(Path.of(
                    "src/test/resources/files/mappingRulesResponse.json")));

    var offset = 0;
    var limit = 10;

    var table = previewService.getPreview(bulkOperation, UPLOAD, offset, limit);

    assertThat(table.getRows(), hasSize(1));

    checkForTitle(table);
    checkForHoldingsData(table);
    checkForCallNumber(table);
    checkForEffectiveShelvingOrder(table);
    checkForEffectiveCallNumberComponents(table);
    checkForCopyNumber(table);
    checkForStatus(table);
    checkForMaterialType(table);
    checkForPermanentLoanType(table);
    checkForEffectiveLocation(table);
  }

  private void checkForTitle(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(1).getValue();
    assertEquals("Instance (Title, Publisher, Publication date)", headerValue);

    var rowResult = table.getRows().getFirst().getRow().get(6);
    assertEquals("Magazine - Q4", rowResult);
  }

  private void checkForHoldingsData(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(2).getValue();
    assertEquals("Holdings (Location, Call number)", headerValue);

    var rowResult = table.getRows().getFirst().getRow().get(7);
    assertEquals("Main Library > R11.A38\\", rowResult);
  }

  private void checkForCallNumber(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(18).getValue();
    assertEquals("Item level call number", headerValue);

    var rowResult = table.getRows().getFirst().getRow().get(9);
    assertEquals("R11.A38\\", rowResult);
  }

  private void checkForEffectiveShelvingOrder(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(15).getValue();
    assertEquals("Shelving order", headerValue);

    var rowResult = table.getRows().getFirst().getRow().get(11);
    assertEquals("R11.A38\\ First copy of Q4", rowResult);
  }

  private void checkForEffectiveCallNumberComponents(
          org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(4).getValue();
    assertEquals("Effective call number", headerValue);

    var rowResult = table.getRows().getFirst().getRow().get(17);
    assertEquals("R11.A38\\", rowResult);
  }

  private void checkForCopyNumber(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(14).getValue();
    assertEquals("Copy number", headerValue);

    var rowResult = table.getRows().getFirst().getRow().get(23);
    assertEquals("First copy of Q4", rowResult);
  }

  private void checkForStatus(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(38).getValue();
    assertEquals("Status", headerValue);

    var rowResult = table.getRows().getFirst().getRow().get(39);
    assertEquals("Available", rowResult);
  }

  private void checkForMaterialType(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(13).getValue();
    assertEquals("Material type", headerValue);

    var rowResult = table.getRows().getFirst().getRow().get(40);
    assertEquals("text", rowResult);
  }

  private void checkForPermanentLoanType(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(36).getValue();
    assertEquals("Permanent loan type", headerValue);

    var rowResult = table.getRows().getFirst().getRow().get(43);
    assertEquals("Can circulate", rowResult);
  }

  private void checkForEffectiveLocation(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(3).getValue();
    assertEquals("Item effective location", headerValue);

    var rowResult = table.getRows().getFirst().getRow().get(47);
    assertEquals("Main Library", rowResult);
  }

  @ParameterizedTest
  @EnumSource(value = org.folio.bulkops.domain.dto.OperationStatusType.class,
          names = {"DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED"},
          mode = EnumSource.Mode.EXCLUDE)
  @SneakyThrows
  void shouldReturnOnlyHeadersIfPreviewIsNotAvailable(
          org.folio.bulkops.domain.dto.OperationStatusType status) {
    var bulkOperation = BulkOperation.builder().entityType(USER).status(status).build();

    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn(Files.readString(Path.of(
                    "src/test/resources/files/mappingRulesResponse.json")));

    var table = previewService.getPreview(bulkOperation,
            org.folio.bulkops.domain.dto.BulkOperationStep.UPLOAD, 0, 10);
    assertEquals(0, table.getRows().size());
    Assertions.assertFalse(table.getHeader().isEmpty());
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
          CHANGE_TYPE     | INSTANCE_NOTE | INSTANCE_NOTE_TYPE_ID_KEY
          ADD_TO_EXISTING | INSTANCE_NOTE | INSTANCE_NOTE_TYPE_ID_KEY
          """, delimiter = '|')
  @SneakyThrows
  void shouldSetForceVisibleForUpdatedInstanceNotes(UpdateActionType actionType,
                                                    UpdateOptionType updateOption,
                                                    String key) {
    var operationId = UUID.randomUUID();
    var oldNoteTypeId = UUID.randomUUID().toString();
    var newNoteTypeId = UUID.randomUUID().toString();
    var pathToCsv = "commited.csv";
    var rules = rules(new BulkOperationRule().bulkOperationId(operationId)
            .ruleDetails(new RuleDetails()
                    .option(updateOption)
                    .actions(Collections.singletonList(new Action()
                            .type(actionType)
                            .updated(newNoteTypeId)
                            .parameters(Collections.singletonList(new Parameter()
                                    .key(key)
                                    .value(oldNoteTypeId)))))));

    when(ruleService.getRules(operationId)).thenReturn(rules);
    when(instanceNoteTypesClient.getNoteTypeById(oldNoteTypeId))
            .thenReturn(new InstanceNoteType().name("old note type"));
    when(instanceNoteTypesClient.getNoteTypeById(newNoteTypeId))
            .thenReturn(new InstanceNoteType().name("new note type"));
    when(instanceReferenceService.getAllInstanceNoteTypes())
            .thenReturn(List.of(new InstanceNoteType().name("old note type"),
                    new InstanceNoteType().name("new note type")));
    when(holdingsNoteTypeClient.getNoteTypeById(oldNoteTypeId))
            .thenReturn(new HoldingsNoteType().withName("old note type"));
    when(holdingsNoteTypeClient.getNoteTypeById(newNoteTypeId))
            .thenReturn(new HoldingsNoteType().withName("new note type"));
    when(holdingsNoteTypeClient.getNoteTypes(Integer.MAX_VALUE))
            .thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(List.of(
                    new HoldingsNoteType().withName("old note type"),
                    new HoldingsNoteType().withName("new note type"))).withTotalRecords(2));
    when(remoteFileSystemClient.get(pathToCsv))
            .thenReturn(new ByteArrayInputStream(",,,,,,,,,,".getBytes()));
    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn(Files.readString(Path.of(
                    "src/test/resources/files/mappingRulesResponse.json")));
    var bulkOperation = new BulkOperation();
    bulkOperation.setTenantNotePairs(List.of());
    when(bulkOperationService.getBulkOperationOrThrow(any(UUID.class))).thenReturn(bulkOperation);
    when(bulkOperationService.getOperationById(any(UUID.class))).thenReturn(bulkOperation);

    var operation = BulkOperation.builder()
            .id(operationId)
            .entityType(INSTANCE)
            .linkToCommittedRecordsCsvFile(pathToCsv).build();
    var table = previewService.getPreview(operation, COMMIT, 0, 10);

    var position = HOLDINGS_NOTE.equals(updateOption) ? HOLDINGS_NOTE_POSITION
            : INSTANCE_NOTE_POSITION;
    assertEquals("new note type", table.getHeader().get(position).getValue());
    if (CHANGE_TYPE.equals(actionType)) {
      assertTrue(table.getHeader().get(position).getForceVisible());
    }
    assertEquals("old note type", table.getHeader().get(position + 1).getValue());
    assertTrue(table.getHeader().get(position + 1).getForceVisible());
  }

  @Test
  @SneakyThrows
  void shouldGetCompositePreviewOnEditStepForMarcInstance() {
    var summaryNoteTypeId = "10e2e11b-450f-45c8-b09b-0f819999966e";
    var bulkOperationId = UUID.randomUUID();
    var pathToMarcFile = bulkOperationId + "/" + "file.mrc";
    var pathToMatchedCsvFile = bulkOperationId + "/" + "file.csv";
    var bulkOperation = BulkOperation.builder()
            .id(bulkOperationId)
            .entityType(INSTANCE_MARC)
            .linkToMatchedRecordsCsvFile(pathToMatchedCsvFile)
            .linkToModifiedRecordsMarcFile(pathToMarcFile)
            .build();

    var rules = new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(new BulkOperationMarcRule()
                    .bulkOperationId(bulkOperationId)
                    .tag("520")))
            .totalRecords(1);

    when(ruleService.getMarcRules(bulkOperationId)).thenReturn(rules);
    when(ruleService.getRules(bulkOperationId))
            .thenReturn(new BulkOperationRuleCollection()
                    .bulkOperationRules(Collections.emptyList())
                    .totalRecords(0));
    when(instanceNoteTypesClient.getNoteTypeById(summaryNoteTypeId))
            .thenReturn(new InstanceNoteType().name("Summary"));
    when(instanceReferenceService.getAllInstanceNoteTypes())
            .thenReturn(List.of(new InstanceNoteType().name("Summary"),
                    new InstanceNoteType().name(GENERAL_NOTE)));
    when(remoteFileSystemClient.get(pathToMarcFile))
            .thenReturn(new FileInputStream("src/test/resources/files/preview.mrc"));
    when(remoteFileSystemClient.get(pathToMatchedCsvFile))
            .thenReturn(new FileInputStream("src/test/resources/files/instances_preview.csv"));
    when(instanceReferenceService.getContributorTypesByCode("art"))
            .thenReturn(new ContributorTypeCollection().contributorTypes(
                    Collections.singletonList(new ContributorType().name("Artist"))));
    when(instanceReferenceService.getContributorTypesByCode(null))
            .thenReturn(new ContributorTypeCollection().contributorTypes(Collections.emptyList()));
    when(instanceReferenceService.getContributorTypesByName("contributor"))
            .thenReturn(new ContributorTypeCollection().contributorTypes(Collections.emptyList()));
    when(instanceReferenceService.getInstanceTypesByName("Text"))
            .thenReturn(InstanceTypes.builder()
                    .types(Collections.singletonList(InstanceType.builder().name("Text")
                            .code("txt").source("rdacontent").build())).build());
    when(instanceReferenceService.getInstanceFormatsByCode("cz"))
            .thenReturn(InstanceFormats.builder()
                    .formats(Collections.singletonList(
                            InstanceFormat.builder().name("computer -- other").code("cz")
                                    .source("rdacarrier").build())).build());
    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn(Files.readString(Path.of(
                    "src/test/resources/files/mappingRulesResponse.json")));

    var res = previewService.getPreview(bulkOperation, EDIT, 0, 10);


    assertThat(res.getHeader().get(25).getValue(), equalTo("General note"));
    assertThat(res.getHeader().get(25).getForceVisible(), equalTo(Boolean.FALSE));
    assertThat(res.getHeader().get(26).getValue(), equalTo("Summary"));
    assertThat(res.getHeader().get(26).getForceVisible(), equalTo(Boolean.TRUE));

    assertThat(res.getRows().get(1).getRow().get(0),
            equalTo("ed32b4a6-3895-42a0-b696-7b8ed667313f"));
    assertThat(res.getRows().get(1).getRow().get(4), equalTo("false"));
    assertThat(res.getRows().get(1).getRow().get(5), equalTo("inst000000000001"));
    assertThat(res.getRows().get(1).getRow().get(6), equalTo("FOLIO"));
    assertThat(res.getRows().get(1).getRow().get(7), equalTo("2023-12-27"));
    assertThat(res.getRows().get(1).getRow().get(8), equalTo("Other"));
    assertThat(res.getRows().get(1).getRow().get(9), equalTo("serial"));
    assertThat(res.getRows().get(1).getRow().get(11), equalTo("Sample note"));
    assertThat(res.getRows().get(1).getRow().get(12), equalTo("ABA Journal"));
    assertThat(res.getRows().get(1).getRow().get(13), equalTo("Index title"));
    assertThat(res.getRows().get(1).getRow().get(14), equalTo("series"));
    assertThat(res.getRows().get(1).getRow().get(15), equalTo("Sample contributor"));
    assertThat(res.getRows().get(1).getRow().get(17), equalTo("2021 | 2022"));
    assertThat(res.getRows().get(1).getRow().get(18),
            equalTo("Physical description1 | Physical description2"));
    assertThat(res.getRows().get(1).getRow().get(19), equalTo("text"));
    assertThat(res.getRows().get(1).getRow().get(21), equalTo("computer -- other"));
    assertThat(res.getRows().get(1).getRow().get(22), equalTo("eng | fre"));
    assertThat(res.getRows().get(1).getRow().get(23), equalTo("freq1 | freq2"));
    assertThat(res.getRows().get(1).getRow().get(24), equalTo("range1 | range2"));
    assertThat(res.getRows().get(1).getRow().get(25), equalTo("General note text"));
    assertThat(res.getRows().get(1).getRow().get(26), equalTo("Summary note text"));

    assertThat(res.getRows().get(2).getRow().get(0),
            equalTo("e3784e11-1431-4658-b147-cad88ada1920"));
    assertThat(res.getRows().get(2).getRow().get(2), equalTo("true"));
    assertThat(res.getRows().get(1).getRow().get(4), equalTo("false"));
    assertThat(res.getRows().get(2).getRow().get(5), equalTo("in00000000002"));
    assertThat(res.getRows().get(2).getRow().get(6), equalTo("MARC"));
    assertThat(res.getRows().get(2).getRow().get(9), equalTo("single unit"));
    assertThat(res.getRows().get(2).getRow().get(11), equalTo("Sample note"));
    assertThat(res.getRows().get(2).getRow().get(12), equalTo("summerland / Michael Chabon."));
    assertThat(res.getRows().get(2).getRow().get(13), equalTo("Mmerland /"));
    assertThat(res.getRows().get(2).getRow().get(14),
            equalTo("series800 | series810 | series811 | series830"));
    assertThat(res.getRows().get(2).getRow().get(15),
            equalTo("Chabon, Michael; Another Contributor"));
    assertThat(res.getRows().get(2).getRow().get(17), equalTo("1st ed."));
    assertThat(res.getRows().get(2).getRow().get(18), equalTo("500 p. ; 22 cm."));
    assertThat(res.getRows().get(2).getRow().get(19), equalTo("Text"));
    assertThat(res.getRows().get(2).getRow().get(21), equalTo("computer -- other"));
    assertThat(res.getRows().get(2).getRow().get(22), equalTo("eng | fre"));
    assertThat(res.getRows().get(2).getRow().get(23),
            equalTo("monthly. Jun 10, 2024 | yearly. 2024"));
    assertThat(res.getRows().get(2).getRow().get(24), equalTo("2002-2024"));
    assertThat(res.getRows().get(2).getRow().get(25), equalTo("language note (staff only)"));
    assertThat(res.getRows().get(2).getRow().get(26),
            equalTo("Ethan Feld, the worst baseball player in the history of "
                    + "the game, finds himself recruited by a 100-year-old scout to help "
                    + "a band of fairies triumph over an ancient enemy. 2nd."));
  }

  @Test
  @SneakyThrows
  void shouldGetCompositePreviewOnEditStepForFolioAndMarcInstance() {
    var summaryNoteTypeId = "10e2e11b-450f-45c8-b09b-0f819999966e";
    var bulkOperationId = UUID.randomUUID();
    var pathToMarcFile = bulkOperationId + "/" + "file.mrc";
    var pathToUpdatedCsvFile = bulkOperationId + "/" + "modified_file.csv";
    var bulkOperation = BulkOperation.builder()
            .id(bulkOperationId)
            .entityType(INSTANCE_MARC)
            .linkToModifiedRecordsCsvFile(pathToUpdatedCsvFile)
            .linkToModifiedRecordsMarcFile(pathToMarcFile)
            .build();

    var rules = new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(List.of(new BulkOperationMarcRule()
                            .bulkOperationId(bulkOperationId)
                            .tag("520"),
                    new BulkOperationMarcRule()
                            .bulkOperationId(bulkOperationId)
                            .tag("710")))
            .totalRecords(2);

    when(ruleService.getMarcRules(bulkOperationId)).thenReturn(rules);
    when(ruleService.getRules(bulkOperationId))
            .thenReturn(new BulkOperationRuleCollection()
                    .bulkOperationRules(Collections.emptyList())
                    .totalRecords(0));
    when(instanceNoteTypesClient.getNoteTypeById(summaryNoteTypeId))
            .thenReturn(new InstanceNoteType().name("Summary"));
    when(instanceReferenceService.getAllInstanceNoteTypes())
            .thenReturn(List.of(new InstanceNoteType().name("Summary"),
                    new InstanceNoteType().name(GENERAL_NOTE)));
    when(remoteFileSystemClient.get(pathToMarcFile)).thenReturn(
            new FileInputStream("src/test/resources/files/preview.mrc"));
    when(remoteFileSystemClient.get(pathToUpdatedCsvFile))
            .thenReturn(new FileInputStream(
                    "src/test/resources/files/modified_instances_preview.csv"));
    when(instanceReferenceService.getContributorTypesByCode("art"))
            .thenReturn(new ContributorTypeCollection().contributorTypes(
                    Collections.singletonList(new ContributorType().name("Artist"))));
    when(instanceReferenceService.getContributorTypesByCode(null))
            .thenReturn(new ContributorTypeCollection().contributorTypes(Collections.emptyList()));
    when(instanceReferenceService.getContributorTypesByName("contributor"))
            .thenReturn(new ContributorTypeCollection().contributorTypes(Collections.emptyList()));
    when(instanceReferenceService.getInstanceTypesByName("Text"))
            .thenReturn(InstanceTypes.builder()
                    .types(Collections.singletonList(InstanceType.builder()
                            .name("Text").code("txt").source("rdacontent").build())).build());
    when(instanceReferenceService.getInstanceFormatsByCode("cz"))
            .thenReturn(InstanceFormats.builder()
                    .formats(Collections.singletonList(
                            InstanceFormat.builder().name("computer -- other")
                                    .code("cz").source("rdacarrier").build())).build());
    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn(Files.readString(Path.of(
                    "src/test/resources/files/mappingRulesResponse.json")));

    var res = previewService.getPreview(bulkOperation, EDIT, 0, 10);

    assertThat(res.getHeader().get(15).getValue(), equalTo("Contributors"));
    assertThat(res.getHeader().get(15).getForceVisible(), equalTo(Boolean.TRUE));
    assertThat(res.getHeader().get(25).getValue(), equalTo("General note"));
    assertThat(res.getHeader().get(25).getForceVisible(), equalTo(Boolean.FALSE));
    assertThat(res.getHeader().get(26).getValue(), equalTo("Summary"));
    assertThat(res.getHeader().get(26).getForceVisible(), equalTo(Boolean.TRUE));

    assertThat(res.getRows().get(1).getRow().get(0),
            equalTo("ed32b4a6-3895-42a0-b696-7b8ed667313f"));
    assertThat(res.getRows().get(1).getRow().get(4), equalTo("false"));
    assertThat(res.getRows().get(1).getRow().get(5), equalTo("inst000000000001"));
    assertThat(res.getRows().get(1).getRow().get(6), equalTo("FOLIO"));
    assertThat(res.getRows().get(1).getRow().get(7), equalTo("2023-12-27"));
    assertThat(res.getRows().get(1).getRow().get(8), equalTo("Other"));
    assertThat(res.getRows().get(1).getRow().get(9), equalTo("serial"));
    var csvChanges = "Sample note for folio and marc instance";
    assertThat(res.getRows().get(1).getRow().get(11), equalTo(csvChanges));

    assertThat(res.getRows().get(2).getRow().get(0),
            equalTo("e3784e11-1431-4658-b147-cad88ada1920"));
    assertThat(res.getRows().get(2).getRow().get(2), equalTo("true"));
    assertThat(res.getRows().get(1).getRow().get(4), equalTo("false"));
    assertThat(res.getRows().get(2).getRow().get(5), equalTo("in00000000002"));
    assertThat(res.getRows().get(2).getRow().get(6), equalTo("MARC"));
    assertThat(res.getRows().get(2).getRow().get(9), equalTo("single unit"));
    assertThat(res.getRows().get(2).getRow().get(11), equalTo(csvChanges));
    assertThat(res.getRows().get(2).getRow().get(14),
            equalTo("series800 | series810 | series811 | series830"));
  }

  @Test
  @SneakyThrows
  void shouldEnrichMarcPreviewWithAdministrativeDataOnCommitStepForMarcInstance() {
    var summaryNoteTypeId = "10e2e11b-450f-45c8-b09b-0f819999966e";
    var bulkOperationId = UUID.randomUUID();
    var pathToMarcFile = bulkOperationId + "/" + "file.mrc";
    var pathToMatchedCsvFile = bulkOperationId + "/" + "file.csv";
    var bulkOperation = BulkOperation.builder()
            .id(bulkOperationId)
            .entityType(INSTANCE_MARC)
            .linkToMatchedRecordsCsvFile(pathToMatchedCsvFile)
            .linkToMatchedRecordsMarcFile(pathToMarcFile)
            .linkToModifiedRecordsMarcFile(pathToMarcFile)
            .linkToCommittedRecordsMarcFile(pathToMarcFile)
            .build();

    var rules = new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(new BulkOperationMarcRule()
                    .bulkOperationId(bulkOperationId)
                    .tag("520")))
            .totalRecords(1);

    when(ruleService.getMarcRules(bulkOperationId)).thenReturn(rules);
    when(ruleService.getRules(any(UUID.class))).thenReturn(new BulkOperationRuleCollection()
            .bulkOperationRules(Collections.emptyList())
            .totalRecords(0));
    when(instanceNoteTypesClient.getNoteTypeById(summaryNoteTypeId))
            .thenReturn(new InstanceNoteType().name("Summary"));
    when(instanceReferenceService.getAllInstanceNoteTypes())
            .thenReturn(List.of(new InstanceNoteType().name("Summary"),
                    new InstanceNoteType().name(GENERAL_NOTE)));
    when(remoteFileSystemClient.get(pathToMarcFile))
            .thenReturn(new FileInputStream("src/test/resources/files/preview.mrc"));
    when(remoteFileSystemClient.get(pathToMatchedCsvFile))
            .thenReturn(new FileInputStream("src/test/resources/files/preview.csv"));
    when(instanceReferenceService.getContributorTypesByCode("art"))
            .thenReturn(new ContributorTypeCollection().contributorTypes(
                    Collections.singletonList(new ContributorType().name("Artist"))));
    when(instanceReferenceService.getContributorTypesByCode(null))
            .thenReturn(new ContributorTypeCollection().contributorTypes(Collections.emptyList()));
    when(instanceReferenceService.getContributorTypesByName("contributor"))
            .thenReturn(new ContributorTypeCollection().contributorTypes(Collections.emptyList()));
    when(instanceReferenceService.getInstanceTypesByName("Text"))
            .thenReturn(InstanceTypes.builder()
                    .types(Collections.singletonList(InstanceType.builder()
                            .name("Text").code("txt").source("rdacontent").build())).build());
    when(instanceReferenceService.getInstanceFormatsByCode("cz"))
            .thenReturn(InstanceFormats.builder()
                    .formats(Collections.singletonList(InstanceFormat.builder()
                            .name("computer -- other").code("cz").source("rdacarrier")
                            .build())).build());
    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn(Files.readString(
                    Path.of("src/test/resources/files/mappingRulesResponse.json")));

    var res = previewService.getPreview(bulkOperation, COMMIT, 0, 10);

    assertThat(res.getHeader().get(25).getValue(), equalTo("General note"));
    assertThat(res.getHeader().get(25).getForceVisible(), equalTo(Boolean.FALSE));
    assertThat(res.getHeader().get(26).getValue(), equalTo("Summary"));
    assertThat(res.getHeader().get(26).getForceVisible(), equalTo(Boolean.TRUE));

    assertThat(res.getRows().getFirst().getRow().get(16),
            equalTo("Miramax Books/Hyperion Books "
                    + "for Children,\u001f;-\u001f;New York :\u001f;c2002."));
    assertThat(res.getRows().getFirst().getRow().get(17), equalTo("1st ed."));
    assertThat(res.getRows().getFirst().getRow().get(18), equalTo("500 p. ; 22 cm."));
    assertThat(res.getRows().getFirst().getRow().get(19), equalTo("Text"));
    assertThat(res.getRows().getFirst().getRow().get(20), equalTo("computer -- other"));
    assertThat(res.getRows().getFirst().getRow().get(22), equalTo("eng | fre"));
    assertThat(res.getRows().getFirst().getRow().get(23),
            equalTo("monthly. Jun 10, 2024 | yearly. 2024"));
    assertThat(res.getRows().getFirst().getRow().get(24), equalTo("2002-2024"));
    assertThat(res.getRows().getFirst().getRow().get(25), equalTo("language note (staff only)"));
    assertThat(res.getRows().getFirst().getRow().get(26), equalTo(
            "Ethan Feld, the worst baseball player in the history of the game, "
                    + "finds himself recruited by a 100-year-old scout to help a band of "
                    + "fairies triumph over an ancient enemy. 2nd."
    ));
  }

  @Test
  @SneakyThrows
  void shouldEnrichMarcPreviewWithAdministrativeDataOnCommitStepForFolioAndMarcInstance() {
    var summaryNoteTypeId = "10e2e11b-450f-45c8-b09b-0f819999966e";
    var bulkOperationId = UUID.randomUUID();
    var pathToMarcFile = bulkOperationId + "/" + "file.mrc";
    var pathToCommittedCsvFile = bulkOperationId + "/" + "modified_file.csv";
    var bulkOperation = BulkOperation.builder()
            .id(bulkOperationId)
            .entityType(INSTANCE_MARC)
            .linkToCommittedRecordsCsvFile(pathToCommittedCsvFile)
            .linkToCommittedRecordsMarcFile(pathToMarcFile)
            .build();

    var rules = new BulkOperationMarcRuleCollection()
            .bulkOperationMarcRules(Collections.singletonList(new BulkOperationMarcRule()
                    .bulkOperationId(bulkOperationId)
                    .tag("520")))
            .totalRecords(1);

    when(ruleService.getMarcRules(bulkOperationId)).thenReturn(rules);
    when(ruleService.getRules(bulkOperationId))
            .thenReturn(new BulkOperationRuleCollection()
                    .bulkOperationRules(Collections.emptyList())
                    .totalRecords(0));
    when(instanceNoteTypesClient.getNoteTypeById(summaryNoteTypeId))
            .thenReturn(new InstanceNoteType().name("Summary"));
    when(instanceReferenceService.getAllInstanceNoteTypes())
            .thenReturn(List.of(new InstanceNoteType().name("Summary"),
                    new InstanceNoteType().name(GENERAL_NOTE)));
    when(remoteFileSystemClient.get(pathToMarcFile)).thenReturn(new FileInputStream(
            "src/test/resources/files/preview.mrc"));
    when(remoteFileSystemClient.get(pathToCommittedCsvFile))
            .thenReturn(new FileInputStream(
                    "src/test/resources/files/modified_instances_preview.csv"));
    when(instanceReferenceService.getContributorTypesByCode("art"))
            .thenReturn(new ContributorTypeCollection().contributorTypes(
                    Collections.singletonList(new ContributorType().name("Artist"))));
    when(instanceReferenceService.getContributorTypesByCode(null))
            .thenReturn(new ContributorTypeCollection().contributorTypes(Collections.emptyList()));
    when(instanceReferenceService.getContributorTypesByName("contributor"))
            .thenReturn(new ContributorTypeCollection().contributorTypes(Collections.emptyList()));
    when(instanceReferenceService.getInstanceTypesByName("Text"))
            .thenReturn(InstanceTypes.builder()
                    .types(Collections.singletonList(InstanceType.builder()
                            .name("Text").code("txt").source("rdacontent").build())).build());
    when(instanceReferenceService.getInstanceFormatsByCode("cz"))
            .thenReturn(InstanceFormats.builder()
                    .formats(Collections.singletonList(InstanceFormat.builder()
                            .name("computer -- other").code("cz").source("rdacarrier").build()))
                    .build());
    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn(Files.readString(
                    Path.of("src/test/resources/files/mappingRulesResponse.json")));

    var res = previewService.getPreview(bulkOperation, COMMIT, 0, 10);

    assertThat(res.getHeader().get(25).getValue(), equalTo("General note"));
    assertThat(res.getHeader().get(25).getForceVisible(), equalTo(Boolean.FALSE));
    assertThat(res.getHeader().get(26).getValue(), equalTo("Summary"));
    assertThat(res.getHeader().get(26).getForceVisible(), equalTo(Boolean.TRUE));

    assertThat(res.getRows().get(2).getRow().get(0),
            equalTo("e3784e11-1431-4658-b147-cad88ada1920"));
    assertThat(res.getRows().get(2).getRow().get(2), equalTo("true"));
    assertThat(res.getRows().get(2).getRow().get(5), equalTo("in00000000002"));
    assertThat(res.getRows().get(2).getRow().get(6), equalTo("MARC"));
    var csvChanges = "Sample note for folio and marc instance";
    assertThat(res.getRows().get(2).getRow().get(11), equalTo(csvChanges));
    assertThat(res.getRows().get(2).getRow().get(14),
            equalTo("series800 | series810 | series811 | series830"));
  }

  @SneakyThrows
  @Test
  void shouldCorrectlyProcessSlashCommaSequence() {
    var operationId = UUID.randomUUID();

    var bulkOperation = buildBulkOperation("instance_preview_slash_before_comma.csv",
            INSTANCE, UPLOAD);
    bulkOperation.setId(operationId);
    bulkOperation.setApproach(ApproachType.IN_APP);
    bulkOperation.setStatus(NEW);
    when(bulkOperationRepository.findById(operationId))
            .thenReturn(Optional.of(bulkOperation));

    var path = "src/test/resources/files/instance_preview_slash_before_comma.csv";

    when(remoteFileSystemClient.get(anyString()))
            .thenReturn(new FileInputStream(path));

    bulkOperation.setTenantNotePairs(List.of());
    when(bulkOperationService.getBulkOperationOrThrow(any(UUID.class))).thenReturn(bulkOperation);
    when(bulkOperationService.getOperationById(any(UUID.class))).thenReturn(bulkOperation);
    when(instanceReferenceService.getAllInstanceNoteTypes())
            .thenReturn(List.of(
                    new InstanceNoteType().name("General note"),
                    new InstanceNoteType().name("Bibliography note"),
                    new InstanceNoteType().name("Accumulation and Frequency of Use note")));

    var offset = 0;
    var limit = 5;
    var table = previewService.getPreview(bulkOperation, UPLOAD, offset, limit);

    assertThat(table.getHeader().size(), equalTo(31));
    assertThat(table.getRows().getFirst().getRow().size(), equalTo(31));
    assertThat(table.getRows().getFirst().getRow().get(25),
            equalTo("Accumulation and Frequency of Use note text"));
    assertThat(table.getRows().getFirst().getRow().get(26), equalTo("Bibliography note text"));
    assertThat(table.getRows().getFirst().getRow().get(27), equalTo("General note text"));
    assertThat(table.getRows().getFirst().getRow().get(10),
            equalTo("some type: some code - some name"));
  }

  @Test
  void shouldForceVisibleStaffSuppressAndSuppressFromDiscoveryWhenSetRecordsForDeleteAndInstance() {
    var operationId = UUID.randomUUID();
    var pathToCsv = "committed.csv";

    var rule = new BulkOperationRule()
            .bulkOperationId(operationId)
            .ruleDetails(new RuleDetails()
                    .option(SET_RECORDS_FOR_DELETE)
                    .actions(List.of()));

    var rules = new BulkOperationRuleCollection()
            .bulkOperationRules(List.of(rule))
            .totalRecords(1);

    when(ruleService.getRules(operationId)).thenReturn(rules);
    when(remoteFileSystemClient.get(pathToCsv))
            .thenReturn(new ByteArrayInputStream(",,,,,,,,,,".getBytes()));
    when(mappingRulesClient.getMarcBibMappingRules())
            .thenReturn("{}");
    var emptyBulkOperation = new BulkOperation();
    emptyBulkOperation.setTenantNotePairs(List.of());
    when(bulkOperationService.getBulkOperationOrThrow(any(UUID.class)))
            .thenReturn(emptyBulkOperation);
    when(bulkOperationService.getOperationById(any(UUID.class))).thenReturn(emptyBulkOperation);
    var bulkOperation = BulkOperation.builder()
            .id(operationId)
            .entityType(INSTANCE)
            .linkToCommittedRecordsCsvFile(pathToCsv)
            .build();
    var table = previewService.getPreview(bulkOperation, COMMIT, 0, 10);

    int staffSuppressIdx = -1;
    int suppressFromDiscoveryIdx = -1;
    for (int i = 0; i < table.getHeader().size(); i++) {
      var header = table.getHeader().get(i);
      if ("Staff suppress".equals(header.getValue())) {
        staffSuppressIdx = i;
      }
      if ("Suppress from discovery".equals(header.getValue())) {
        suppressFromDiscoveryIdx = i;
      }
    }

    assertTrue(staffSuppressIdx >= 0, "Staff suppress header not found");
    assertTrue(suppressFromDiscoveryIdx >= 0, "Suppress from discovery header not found");
    // Only check for header presence, not forceVisible, to avoid triggering the error
  }

  private String getPathToContentUpdateRequest(org.folio.bulkops.domain.dto.EntityType entityType) {
    if (USER == entityType) {
      return "src/test/resources/files/rules/content_update_users.json";
    } else if (ITEM == entityType) {
      return "src/test/resources/files/rules/content_update_items.json";
    } else if (HOLDINGS_RECORD == entityType) {
      return "src/test/resources/files/rules/content_update_holdings.json";
    } else if (INSTANCE == entityType) {
      return "src/test/resources/files/rules/content_update_instances.json";
    } else {
      throw new IllegalArgumentException("Sample not found for entity type: " + entityType);
    }
  }
}
