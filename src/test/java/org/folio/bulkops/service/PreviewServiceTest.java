package org.folio.bulkops.service;

import static java.util.Collections.emptySet;
import static org.folio.bulkops.domain.dto.BulkOperationStep.COMMIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.EDIT;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.util.Constants.HOLDINGS_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.ITEM_NOTE_POSITION;
import static org.folio.bulkops.util.Constants.QUERY_ALL_RECORDS;
import static org.folio.bulkops.util.UnifiedTableHeaderBuilder.getHeaders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.hamcrest.Matchers.equalTo;
import static org.testcontainers.shaded.org.hamcrest.Matchers.hasSize;

import java.io.FileInputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.HoldingsNoteType;
import org.folio.bulkops.domain.bean.HoldingsNoteTypeCollection;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.Instance;
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

    when(itemNoteTypeClient.getByQuery(QUERY_ALL_RECORDS)).thenReturn(new NoteTypeCollection().withItemNoteTypes(List.of(new NoteType().withName("Binding"), new NoteType().withName("Custom"), new NoteType().withName("Provenance"), new NoteType().withName("Reproduction"), new NoteType().withName("Note"))));
    when(holdingsNoteTypeClient.getByQuery(QUERY_ALL_RECORDS)).thenReturn(new HoldingsNoteTypeCollection().withHoldingsNoteTypes(List.of(new HoldingsNoteType().withName("Binding"), new HoldingsNoteType().withName("Provenance"), new HoldingsNoteType().withName("Reproduction"))));

    when(itemNoteTypeClient.getById("0e40884c-3523-4c6d-8187-d578e3d2794e")).thenReturn(new NoteType().withName("Binding"));
    when(itemNoteTypeClient.getById("f3ae3823-d096-4c65-8734-0c1efd2ffea8")).thenReturn(new NoteType().withName("Provenance"));
    when(itemNoteTypeClient.getById("c3a539b9-9576-4e3a-b6de-d910200b2919")).thenReturn(new NoteType().withName("Reproduction"));
    when(itemNoteTypeClient.getById("87c450be-2033-41fb-80ba-dd2409883681")).thenReturn(new NoteType().withName("Custom"));

    when(itemNoteTypeClient.getById("8d0a5eca-25de-4391-81a9-236eeefdd20b")).thenReturn(new NoteType().withName("Note"));

    when(holdingsNoteTypeClient.getById("e19eabab-a85c-4aef-a7b2-33bd9acef24e")).thenReturn(new HoldingsNoteType().withName("Reproduction"));


    var bulkOperationRuleCollection = objectMapper.readValue(new FileInputStream(getPathToContentUpdateRequest(entityType)), BulkOperationRuleCollection.class);
    when(ruleService.getRules(any(UUID.class))).thenReturn(bulkOperationRuleCollection);

    var table = previewService.getPreview(bulkOperation, step, offset, limit);

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
        headers = getHeaders(Item.class, Set.of("Binding", "Custom", "Status", "Check Out Notes", "Provenance", "Reproduction", "Check In Notes", "Note", "Administrative Notes"));
        noteTableUpdater.extendHeadersWithItemNoteTypeNames(ITEM_NOTE_POSITION, headers , List.of("Binding", "Custom", "Note", "Provenance", "Reproduction"), Set.of("Binding","Status","Check Out Notes","Provenance","Check In Notes","Note","Custom", "Reproduction", "Administrative Notes"));
      } else {
        headers = getHeaders(Item.class);
        noteTableUpdater.extendHeadersWithItemNoteTypeNames(ITEM_NOTE_POSITION, headers , List.of("Binding", "Custom", "Note", "Provenance", "Reproduction"), emptySet());
      }
      assertThat(table.getHeader(), equalTo(headers));
    } else if (HOLDINGS_RECORD.equals(entityType)) {
      List<Cell> headers;
      if ((step == EDIT || step == COMMIT) && approachType == ApproachType.IN_APP) {
        headers = getHeaders(HoldingsRecord.class, Set.of("Reproduction", "Discovery suppress", "Electronic access", "Administrative notes"));
        noteTableUpdater.extendHeadersWithItemNoteTypeNames(HOLDINGS_NOTE_POSITION, headers , List.of("Binding", "Provenance", "Reproduction"), Set.of("Reproduction","Discovery suppress","Electronic access","Administrative notes"));
      } else {
        headers = getHeaders(HoldingsRecord.class);
        noteTableUpdater.extendHeadersWithItemNoteTypeNames(HOLDINGS_NOTE_POSITION, headers , List.of("Binding", "Provenance", "Reproduction"), emptySet());
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

    var table = previewService.getPreview(bulkOperation, COMMIT, offset, limit);

    assertThat(table.getRows(), hasSize(limit));
  }

  @ParameterizedTest
  @EnumSource(value = org.folio.bulkops.domain.dto.OperationStatusType.class, names = { "DATA_MODIFICATION", "REVIEW_CHANGES", "COMPLETED" }, mode = EnumSource.Mode.EXCLUDE)
  @SneakyThrows
  void shouldReturnOnlyHeadersIfPreviewIsNotAvailable(org.folio.bulkops.domain.dto.OperationStatusType status) {

    var bulkOperation = BulkOperation.builder().entityType(USER).status(status).build();

    var table = previewService.getPreview(bulkOperation, org.folio.bulkops.domain.dto.BulkOperationStep.UPLOAD, 0, 10);
    assertEquals(0, table.getRows().size());
    Assertions.assertTrue(table.getHeader().size() > 0);
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
