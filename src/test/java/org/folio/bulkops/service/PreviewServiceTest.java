package org.folio.bulkops.service;

import static java.util.Collections.emptySet;
import static org.folio.bulkops.domain.dto.BulkOperationStep.COMMIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.EDIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.UPLOAD;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.HOLDINGS_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.INSTANCE_NOTE;
import static org.folio.bulkops.processor.InstanceNotesUpdaterFactory.INSTANCE_NOTE_TYPE_ID_KEY;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.folio.bulkops.BaseTest;
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
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.Cell;
import org.folio.bulkops.domain.dto.ContributorType;
import org.folio.bulkops.domain.dto.ContributorTypeCollection;
import org.folio.bulkops.domain.dto.InstanceNoteType;
import org.folio.bulkops.domain.dto.Parameter;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.BulkOperationStep;

import lombok.SneakyThrows;

class PreviewServiceTest extends BaseTest {

  @Autowired
  private PreviewService previewService;
  @MockBean
  private BulkOperationRepository bulkOperationRepository;
  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockBean
  private RuleService ruleService;
  @MockBean
  private InstanceReferenceService instanceReferenceService;
  @Autowired
  private NoteTableUpdater noteTableUpdater;

  @CsvSource(value = { "users_preview.csv,USER,UPLOAD,IN_APP",
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
  void shouldReturnPreviewIfAvailable(String fileName, EntityType entityType, BulkOperationStep step, ApproachType approachType) {
    var path = "src/test/resources/files/" + fileName;
    var operationId = UUID.randomUUID();
    var offset = 2;
    var limit = 5;

    var bulkOperation = buildBulkOperation(fileName, entityType, step);
    bulkOperation.setId(operationId);
    bulkOperation.setApproach(approachType);
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(bulkOperation));

    when(remoteFileSystemClient.get(anyString()))
      .thenReturn(new FileInputStream(path));

    when(groupClient.getGroupById(anyString())).thenReturn(new UserGroup().withGroup("Group"));
    when(locationClient.getLocationById(anyString())).thenReturn(new ItemLocation().withName("Location"));
    when(holdingsSourceClient.getById(anyString())).thenReturn(new HoldingsRecordsSource().withName("Source"));

    when(itemNoteTypeClient.getNoteTypes(Integer.MAX_VALUE)).thenReturn(new NoteTypeCollection().withItemNoteTypes(List.of(new NoteType().withName("Binding"), new NoteType().withName("Custom"), new NoteType().withName("Provenance"), new NoteType().withName("Reproduction"), new NoteType().withName("Note"))));
    when(holdingsNoteTypeClient.getNoteTypes(Integer.MAX_VALUE)).thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(List.of(new HoldingsNoteType().withName("Binding"), new HoldingsNoteType().withName("Provenance"), new HoldingsNoteType().withName("Reproduction"))));

    when(itemNoteTypeClient.getNoteTypeById("0e40884c-3523-4c6d-8187-d578e3d2794e")).thenReturn(new NoteType().withName("Binding"));
    when(itemNoteTypeClient.getNoteTypeById("f3ae3823-d096-4c65-8734-0c1efd2ffea8")).thenReturn(new NoteType().withName("Provenance"));
    when(itemNoteTypeClient.getNoteTypeById("c3a539b9-9576-4e3a-b6de-d910200b2919")).thenReturn(new NoteType().withName("Reproduction"));
    when(itemNoteTypeClient.getNoteTypeById("87c450be-2033-41fb-80ba-dd2409883681")).thenReturn(new NoteType().withName("Custom"));

    when(itemNoteTypeClient.getNoteTypeById("8d0a5eca-25de-4391-81a9-236eeefdd20b")).thenReturn(new NoteType().withName("Note"));

    when(holdingsNoteTypeClient.getNoteTypeById("e19eabab-a85c-4aef-a7b2-33bd9acef24e")).thenReturn(new HoldingsNoteType().withName("Reproduction"));


    var bulkOperationRuleCollection = objectMapper.readValue(new FileInputStream(getPathToContentUpdateRequest(entityType)), BulkOperationRuleCollection.class);
    when(ruleService.getRules(any(UUID.class))).thenReturn(bulkOperationRuleCollection);

    var table = previewService.getPreview(bulkOperation, step, offset, limit, "FOLIO");

    assertThat(table.getRows(), hasSize(limit - offset));
    if (USER.equals(entityType)) {
      if ((step == EDIT || step == COMMIT) && approachType == ApproachType.IN_APP) {
        assertThat(table.getHeader(), equalTo(
          getHeaders(User.class, UpdateOptionTypeToFieldResolver.getFieldsByUpdateOptionTypes(List.of(UpdateOptionType.EMAIL_ADDRESS, UpdateOptionType.EXPIRATION_DATE), entityType))));
      } else {
        assertThat(table.getHeader(), equalTo(getHeaders(User.class)));
      }
    } else if (ITEM.equals(entityType)) {
      List<Cell> headers;
      if ((step == EDIT || step == COMMIT) && approachType == ApproachType.IN_APP) {
        headers = getHeaders(Item.class, Set.of("Binding", "Custom", "Status", "Check Out Notes", "Provenance", "Reproduction", "Check In Notes", "Note", "Administrative note"));
        noteTableUpdater.extendHeadersWithNoteTypeNames(ITEM_NOTE_POSITION, headers , List.of("Binding", "Custom", "Note", "Provenance", "Reproduction"), Set.of("Binding","Status","Check Out Notes","Provenance","Check In Notes","Note","Custom", "Reproduction", "Administrative notes"));
      } else {
        headers = getHeaders(Item.class);
        noteTableUpdater.extendHeadersWithNoteTypeNames(ITEM_NOTE_POSITION, headers , List.of("Binding", "Custom", "Note", "Provenance", "Reproduction"), emptySet());
      }
      assertThat(table.getHeader(), equalTo(headers));
    } else if (INSTANCE.equals(entityType)) {
      if ((step == EDIT || step == COMMIT) && approachType == ApproachType.IN_APP) {
        assertThat(table.getHeader(), equalTo(
          getHeaders(Instance.class, UpdateOptionTypeToFieldResolver.getFieldsByUpdateOptionTypes(List.of(UpdateOptionType.STAFF_SUPPRESS, UpdateOptionType.SUPPRESS_FROM_DISCOVERY), entityType))));
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
    var offset = 1;
    var limit = 10;

    var bulkOperation = buildBulkOperation("items_preview_line_breaks.csv", ITEM, COMMIT);
    bulkOperation.setId(operationId);
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(bulkOperation));

    when(remoteFileSystemClient.get(anyString()))
      .thenReturn(new FileInputStream(path));

    when(ruleService.getRules(any())).thenReturn(new BulkOperationRuleCollection().bulkOperationRules(List.of(new BulkOperationRule().ruleDetails(new BulkOperationRuleRuleDetails().option(UpdateOptionType.STATUS).actions(List.of(new Action().type(UpdateActionType.REPLACE_WITH).updated("New")))))).totalRecords(1));

    when(locationClient.getLocationById(anyString())).thenReturn(new ItemLocation().withName("Location"));

    var table = previewService.getPreview(bulkOperation, COMMIT, offset, limit, "FOLIO");

    assertThat(table.getRows(), hasSize(limit));
  }

  @Test
  @SneakyThrows
  void shouldProperlyParseContentWithBackSlashes() {
    var path = "src/test/resources/files/items_preview_back_slashes.csv";
    var operationId = UUID.randomUUID();
    var offset = 0;
    var limit = 10;

    var bulkOperation = buildBulkOperation("items_preview_back_slashes.csv", ITEM, UPLOAD);
    bulkOperation.setId(operationId);
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(bulkOperation));

    when(remoteFileSystemClient.get(anyString()))
      .thenReturn(new FileInputStream(path));

    when(ruleService.getRules(any())).thenReturn(new BulkOperationRuleCollection().bulkOperationRules(List.of(new BulkOperationRule().ruleDetails(new BulkOperationRuleRuleDetails().option(UpdateOptionType.STATUS).actions(List.of(new Action().type(UpdateActionType.REPLACE_WITH).updated("New")))))).totalRecords(1));

    when(locationClient.getLocationById(anyString())).thenReturn(new ItemLocation().withName("Location"));

    when(itemNoteTypeClient.getNoteTypes(Integer.MAX_VALUE)).thenReturn(new NoteTypeCollection().withItemNoteTypes(List.of(new NoteType().withName("Binding"), new NoteType().withName("Custom"), new NoteType().withName("Provenance"), new NoteType().withName("Reproduction"), new NoteType().withName("Note"))));

    when(itemNoteTypeClient.getNoteTypeById("0e40884c-3523-4c6d-8187-d578e3d2794e")).thenReturn(new NoteType().withName("Binding"));
    when(itemNoteTypeClient.getNoteTypeById("f3ae3823-d096-4c65-8734-0c1efd2ffea8")).thenReturn(new NoteType().withName("Provenance"));
    when(itemNoteTypeClient.getNoteTypeById("c3a539b9-9576-4e3a-b6de-d910200b2919")).thenReturn(new NoteType().withName("Reproduction"));
    when(itemNoteTypeClient.getNoteTypeById("87c450be-2033-41fb-80ba-dd2409883681")).thenReturn(new NoteType().withName("Custom"));
    when(itemNoteTypeClient.getNoteTypeById("8d0a5eca-25de-4391-81a9-236eeefdd20b")).thenReturn(new NoteType().withName("Note"));

    var table = previewService.getPreview(bulkOperation, UPLOAD, offset, limit, "FOLIO");

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
    var headerValue = table.getHeader().get(6).getValue();
    assertEquals("Title", headerValue);

    var rowResult = table.getRows().get(0).getRow().get(6);
    assertEquals("Magazine - Q4", rowResult);
  }
  private void checkForHoldingsData(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(7).getValue();
    assertEquals("Holdings (Location, Call number)", headerValue);

    var rowResult = table.getRows().get(0).getRow().get(7);
    assertEquals("Main Library > R11.A38\\", rowResult);
  }
  private void checkForCallNumber(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(9).getValue();
    assertEquals("Call Number", headerValue);

    var rowResult = table.getRows().get(0).getRow().get(9);
    assertEquals("R11.A38\\", rowResult);
  }
  private void checkForEffectiveShelvingOrder(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(11).getValue();
    assertEquals("Effective Shelving Order", headerValue);

    var rowResult = table.getRows().get(0).getRow().get(11);
    assertEquals("R11.A38\\ First copy of Q4", rowResult);
  }
  private void checkForEffectiveCallNumberComponents(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(17).getValue();
    assertEquals("Effective Call Number Components", headerValue);

    var rowResult = table.getRows().get(0).getRow().get(17);
    assertEquals("R11.A38\\", rowResult);
  }
  private void checkForCopyNumber(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(23).getValue();
    assertEquals("Copy Number", headerValue);

    var rowResult = table.getRows().get(0).getRow().get(23);
    assertEquals("First copy of Q4", rowResult);
  }
  private void checkForStatus(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(39).getValue();
    assertEquals("Status", headerValue);

    var rowResult = table.getRows().get(0).getRow().get(39);
    assertEquals("Available", rowResult);
  }
  private void checkForMaterialType(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(40).getValue();
    assertEquals("Material Type", headerValue);

    var rowResult = table.getRows().get(0).getRow().get(40);
    assertEquals("text", rowResult);
  }
  private void checkForPermanentLoanType(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(43).getValue();
    assertEquals("Permanent Loan Type", headerValue);

    var rowResult = table.getRows().get(0).getRow().get(43);
    assertEquals("Can circulate", rowResult);
  }
  private void checkForEffectiveLocation(org.folio.bulkops.domain.dto.UnifiedTable table) {
    var headerValue = table.getHeader().get(47).getValue();
    assertEquals("Effective Location", headerValue);

    var rowResult = table.getRows().get(0).getRow().get(47);
    assertEquals("Main Library", rowResult);
  }

  @ParameterizedTest
  @EnumSource(value = org.folio.bulkops.domain.dto.OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.EXCLUDE)
  @SneakyThrows
  void shouldReturnOnlyHeadersIfPreviewIsNotAvailable(org.folio.bulkops.domain.dto.OperationStatusType status) {

    var bulkOperation = BulkOperation.builder().entityType(USER).status(status).build();

    var table = previewService.getPreview(bulkOperation, org.folio.bulkops.domain.dto.BulkOperationStep.UPLOAD, 0, 10, "FOLIO");
    assertEquals(0, table.getRows().size());
    Assertions.assertTrue(table.getHeader().size() > 0);
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
  CHANGE_TYPE     | INSTANCE_NOTE | INSTANCE_NOTE_TYPE_ID_KEY
  ADD_TO_EXISTING | INSTANCE_NOTE | INSTANCE_NOTE_TYPE_ID_KEY
    """, delimiter = '|')
  void shouldSetForceVisibleForUpdatedInstanceNotes(UpdateActionType actionType, UpdateOptionType updateOption,
                                                    String key) {
    var operationId = UUID.randomUUID();
    var oldNoteTypeId = UUID.randomUUID().toString();
    var newNoteTypeId = UUID.randomUUID().toString();
    var pathToCsv = "commited.csv";
    var operation = BulkOperation.builder()
      .id(operationId)
      .entityType(INSTANCE)
      .linkToCommittedRecordsCsvFile(pathToCsv).build();
    var rules = rules(new BulkOperationRule().bulkOperationId(operationId)
      .ruleDetails(new BulkOperationRuleRuleDetails()
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
        new HoldingsNoteType().withName("old note type"), new HoldingsNoteType().withName("new note type"))).withTotalRecords(2));
    when(remoteFileSystemClient.get(pathToCsv))
      .thenReturn(new ByteArrayInputStream(",,,,,,,,,,".getBytes()));

    var table = previewService.getPreview(operation, COMMIT, 0, 10, "FOLIO");

    var position = HOLDINGS_NOTE.equals(updateOption) ? HOLDINGS_NOTE_POSITION : INSTANCE_NOTE_POSITION;
    assertEquals("new note type", table.getHeader().get(position).getValue());
    if (CHANGE_TYPE.equals(actionType)) {
      assertTrue(table.getHeader().get(position).getForceVisible());
    }
    assertEquals("old note type", table.getHeader().get(position + 1).getValue());
    assertTrue(table.getHeader().get(position + 1).getForceVisible());
  }

  @ParameterizedTest
  @EnumSource(value = BulkOperationStep.class, names = {"EDIT", "COMMIT"}, mode = EnumSource.Mode.INCLUDE)
  @SneakyThrows
  void shouldGetMarcPreviewIfAvailable(BulkOperationStep step) {
    var summaryNoteTypeId = "10e2e11b-450f-45c8-b09b-0f819999966e";
    var bulkOperationId = UUID.randomUUID();
    var pathToFile = bulkOperationId + "/" + "file.mrc";
    var bulkOperation = BulkOperation.builder()
        .id(bulkOperationId)
        .entityType(INSTANCE)
        .linkToMatchedRecordsMarcFile(pathToFile)
        .linkToModifiedRecordsMarcFile(pathToFile)
        .linkToCommittedRecordsMarcFile(pathToFile)
      .build();
    var contributorTypes = new HashMap<String, String>();
    contributorTypes.put("art", "Artist");

    var rules = rules(new BulkOperationRule().bulkOperationId(bulkOperationId)
      .ruleDetails(new BulkOperationRuleRuleDetails()
        .option(INSTANCE_NOTE)
        .actions(Collections.singletonList(new Action()
          .type(ADD_TO_EXISTING)
          .updated("new note")
          .parameters(Collections.singletonList(new Parameter()
            .key(INSTANCE_NOTE_TYPE_ID_KEY)
            .value(summaryNoteTypeId)))))));

    when(ruleService.getRules(bulkOperationId)).thenReturn(rules);
    when(instanceNoteTypesClient.getNoteTypeById(summaryNoteTypeId))
      .thenReturn(new InstanceNoteType().name("Summary"));
    when(instanceReferenceService.getAllInstanceNoteTypes())
      .thenReturn(List.of(new InstanceNoteType().name("Summary"), new InstanceNoteType().name(GENERAL_NOTE)));
    when(remoteFileSystemClient.get(pathToFile)).thenReturn(new FileInputStream("src/test/resources/files/preview.mrc"));
    when(instanceReferenceService.getContributorTypesByCode("art"))
      .thenReturn(new ContributorTypeCollection().contributorTypes(
        Collections.singletonList(new ContributorType().name("Artist"))));
    when(instanceReferenceService.getContributorTypesByCode(null))
      .thenReturn(new ContributorTypeCollection().contributorTypes(Collections.emptyList()));
    when(instanceReferenceService.getContributorTypesByName("contributor"))
      .thenReturn(new ContributorTypeCollection().contributorTypes(Collections.emptyList()));
    when(instanceReferenceService.getInstanceTypesByName("Text"))
      .thenReturn(InstanceTypes.builder()
        .types(Collections.singletonList(InstanceType.builder().name("Text").code("txt").source("rdacontent").build())).build());
    when(instanceReferenceService.getInstanceFormatsByCode("cz"))
      .thenReturn(InstanceFormats.builder()
        .formats(Collections.singletonList(InstanceFormat.builder().name("computer -- other").code("cz").source("rdacarrier").build())).build());

    var res = previewService.getPreview(bulkOperation, step, 0, 10, "MARC");

    assertThat(res.getHeader().get(22).getValue(), equalTo("General note"));
    assertThat(res.getHeader().get(22).getForceVisible(), equalTo(Boolean.FALSE));
    assertThat(res.getHeader().get(23).getValue(), equalTo("Summary"));
    assertThat(res.getHeader().get(23).getForceVisible(), equalTo(Boolean.TRUE));

    assertThat(res.getRows().get(0).getRow().get(0), equalTo("e3784e11-1431-4658-b147-cad88ada1920"));
    assertThat(res.getRows().get(0).getRow().get(4), equalTo("in00000000002"));
    assertThat(res.getRows().get(0).getRow().get(5), equalTo("MARC"));
    assertThat(res.getRows().get(0).getRow().get(8), equalTo("single unit"));
    assertThat(res.getRows().get(0).getRow().get(10), equalTo("summerland / Michael Chabon."));
    assertThat(res.getRows().get(0).getRow().get(11), equalTo("Mmerland /"));
    assertThat(res.getRows().get(0).getRow().get(12), equalTo("series800 | series810 | series811 | series830"));
    assertThat(res.getRows().get(0).getRow().get(13), equalTo("Chabon, Michael;Personal name;Artist | Another Contributor;Meeting name;contributor"));
    assertThat(res.getRows().get(0).getRow().get(14), equalTo("1st ed."));
    assertThat(res.getRows().get(0).getRow().get(15), equalTo("500 p. ; 22 cm."));
    assertThat(res.getRows().get(0).getRow().get(16), equalTo("Text;txt;rdacontent"));
    assertThat(res.getRows().get(0).getRow().get(18), equalTo("computer -- other"));
    assertThat(res.getRows().get(0).getRow().get(19), equalTo("English | French"));
    assertThat(res.getRows().get(0).getRow().get(20), equalTo("monthly. Jun 10, 2024 | yearly. 2024"));
    assertThat(res.getRows().get(0).getRow().get(21), equalTo("2002-2024"));
    assertThat(res.getRows().get(0).getRow().get(22), equalTo("language note (staff only)"));
    assertThat(res.getRows().get(0).getRow().get(23), equalTo("Ethan Feld, the worst baseball player in the history of the game, finds himself recruited by a 100-year-old scout to help a band of fairies triumph over an ancient enemy. 2nd"));
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
