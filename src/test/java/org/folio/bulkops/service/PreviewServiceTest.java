package org.folio.bulkops.service;

import static org.folio.bulkops.domain.dto.BulkOperationStep.COMMIT;
import static org.folio.bulkops.domain.dto.BulkOperationStep.EDIT;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.EntityType.ITEM;
import static org.folio.bulkops.domain.dto.EntityType.USER;
import static org.folio.bulkops.util.Constants.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.util.Constants.ADMINISTRATIVE_NOTES;
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
import java.util.UUID;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.RemoteFileSystemClient;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.bean.UserGroup;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
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

import lombok.SneakyThrows;

public class PreviewServiceTest extends BaseTest {

  @Autowired
  private PreviewService previewService;
  @MockBean
  private BulkOperationRepository bulkOperationRepository;
  @MockBean
  private RemoteFileSystemClient remoteFileSystemClient;
  @MockBean
  private RuleService ruleService;
  @MockBean
  private NoteTableUpdater noteTableUpdater;

  @ParameterizedTest
  @CsvSource(value = { "users_preview.csv,USER,UPLOAD",
    "users_preview.csv,USER,EDIT",
    "users_preview.csv,USER,COMMIT",
    "items_preview.csv,ITEM,UPLOAD",
    "items_preview.csv,ITEM,EDIT",
    "items_preview.csv,ITEM,COMMIT",
    "holdings_preview.csv,HOLDINGS_RECORD,UPLOAD",
    "holdings_preview.csv,HOLDINGS_RECORD,EDIT",
    "holdings_preview.csv,HOLDINGS_RECORD,COMMIT"}, delimiter = ',')
  @SneakyThrows
  void shouldReturnPreviewIfAvailable(String fileName, org.folio.bulkops.domain.dto.EntityType entityType, org.folio.bulkops.domain.dto.BulkOperationStep step) {
    var path = "src/test/resources/files/" + fileName;
    var operationId = UUID.randomUUID();
    var offset = 2;
    var limit = 5;

    var bulkOperation = buildBulkOperation(fileName, entityType, step);
    bulkOperation.setId(operationId);
    when(bulkOperationRepository.findById(operationId))
      .thenReturn(Optional.of(bulkOperation));

    when(remoteFileSystemClient.get(anyString()))
      .thenReturn(new FileInputStream(path));

    when(groupClient.getGroupById(anyString())).thenReturn(new UserGroup().withGroup("Group"));
    when(locationClient.getLocationById(anyString())).thenReturn(new ItemLocation().withName("Location"));
    when(holdingsSourceClient.getById(anyString())).thenReturn(new HoldingsRecordsSource().withName("Source"));


    if (USER.equals(entityType)) {
      when(ruleService.getRules(any(UUID.class))).thenReturn(new BulkOperationRuleCollection().bulkOperationRules(
        List.of(new BulkOperationRule().ruleDetails(
          new BulkOperationRuleRuleDetails()
            .option(UpdateOptionType.EMAIL_ADDRESS)
            .actions(List.of(new Action().type(UpdateActionType.REPLACE_WITH).updated("new_mail@mail.net")))
        ))
      ));
    } else if (ITEM.equals(entityType)) {
      when(ruleService.getRules(any(UUID.class))).thenReturn(new BulkOperationRuleCollection().bulkOperationRules(
        List.of(new BulkOperationRule().ruleDetails(
          new BulkOperationRuleRuleDetails()
            .option(UpdateOptionType.STATUS)
            .actions(List.of(new Action().type(UpdateActionType.REPLACE_WITH).updated("new_status")))
        ))
      ));
    } else if (HOLDINGS_RECORD.equals(entityType)) {
      when(ruleService.getRules(any(UUID.class))).thenReturn(new BulkOperationRuleCollection().bulkOperationRules(
        List.of(new BulkOperationRule().ruleDetails(
          new BulkOperationRuleRuleDetails()
            .option(UpdateOptionType.ELECTRONIC_ACCESS_LINK_TEXT)
            .actions(List.of(new Action().type(UpdateActionType.REPLACE_WITH).updated("new_text")))
        ))
      ));
    }



    var table = previewService.getPreview(bulkOperation, step, offset, limit);

    assertThat(table.getRows(), hasSize(limit - offset));
    if (USER.equals(entityType)) {
      if (step == EDIT) {
        assertThat(table.getHeader(), equalTo(
          getHeaders(User.class, UpdateOptionTypeToFieldResolver.getFieldsByUpdateOptionTypes(List.of(UpdateOptionType.EMAIL_ADDRESS)))));
      } else {
        assertThat(table.getHeader(), equalTo(getHeaders(User.class)));
      }
    } else if (org.folio.bulkops.domain.dto.EntityType.ITEM.equals(entityType)) {
      if (step == EDIT) {
        assertThat(table.getHeader(), equalTo(renameAdministrativeNotesHeader(
          getHeaders(Item.class, UpdateOptionTypeToFieldResolver.getFieldsByUpdateOptionTypes(List.of(UpdateOptionType.STATUS))))));
      } else {
        assertThat(table.getHeader(), equalTo(renameAdministrativeNotesHeader(getHeaders(Item.class))));
      }
    } else if (org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD.equals(entityType)) {
      if (step == EDIT) {
        assertThat(table.getHeader(), equalTo(renameAdministrativeNotesHeader(
          getHeaders(HoldingsRecord.class, UpdateOptionTypeToFieldResolver.getFieldsByUpdateOptionTypes(List.of(UpdateOptionType.ELECTRONIC_ACCESS_LINK_TEXT))))));

      } else {
        assertThat(table.getHeader(), equalTo(renameAdministrativeNotesHeader(getHeaders(HoldingsRecord.class))));
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


  private List<org.folio.bulkops.domain.dto.Cell> renameAdministrativeNotesHeader(List<org.folio.bulkops.domain.dto.Cell> headers) {
    headers.forEach(cell -> {
      if (ADMINISTRATIVE_NOTES.equalsIgnoreCase(cell.getValue())) {
        cell.setValue(ADMINISTRATIVE_NOTE);
      }
    });
    return headers;
  }
}
