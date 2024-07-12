package org.folio.bulkops.processor;

import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE_INCLUDING_ITEMS;
import static org.folio.bulkops.util.Constants.APPLY_TO_HOLDINGS;
import static org.folio.bulkops.util.Constants.APPLY_TO_ITEMS;
import static org.folio.bulkops.util.Constants.GET_HOLDINGS_BY_INSTANCE_ID_QUERY;
import static org.folio.bulkops.util.Constants.GET_ITEMS_BY_HOLDING_ID_QUERY;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordCollection;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemCollection;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.BulkOperationStep;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.RuleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

class UpdateProcessorTest extends BaseTest {

  @MockBean
  private RuleService ruleService;
  @SpyBean
  private ItemClient itemClient;
  @SpyBean
  private ErrorService errorService;
  @MockBean
  private BulkOperationExecutionContentRepository executionContentRepository;
  @MockBean
  private BulkOperationRepository bulkOperationRepository;
  @Autowired
  private HoldingsUpdateProcessor holdingsUpdateProcessor;
  @Autowired
  private ItemUpdateProcessor itemUpdateProcessor;
  @Autowired
  private UserUpdateProcessor userUpdateProcessor;
  @Autowired
  private InstanceUpdateProcessor instanceUpdateProcessor;
  @MockBean
  private HoldingsReferenceService holdingsReferenceService;

  @Test
  void shouldUpdateHoldingsRecord() {
    var holdingsRecord = new HoldingsRecord()
        .withId(UUID.randomUUID().toString())
        .withInstanceId(UUID.randomUUID().toString());

    when(ruleService.getRules(isA(UUID.class))).thenReturn(new BulkOperationRuleCollection());
    holdingsUpdateProcessor.updateRecord(holdingsRecord);

    verify(holdingsClient).updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    SET_TO_TRUE   | false
    SET_TO_FALSE  | false
    SET_TO_TRUE   | true
    SET_TO_FALSE  | true
    """, delimiter = '|')
  void holdings_shouldNotUpdateAssociatedItemsDiscoverySuppress(UpdateActionType actionType, boolean notChanged) {
    var holdingsId = UUID.randomUUID().toString();
    var holdingsRecord = HoldingsRecord.builder()
      .id(holdingsId)
      .discoverySuppress(true)
      .build();

    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .identifierType(IdentifierType.ID)
      .build();

    var rule = new BulkOperationRule().ruleDetails(new BulkOperationRuleRuleDetails()
      .option(UpdateOptionType.SUPPRESS_FROM_DISCOVERY)
      .actions(Collections.singletonList(new Action().type(actionType))));
    when(ruleService.getRules(operationId)).thenReturn(new BulkOperationRuleCollection()
      .bulkOperationRules(Collections.singletonList(rule))
      .totalRecords(1));
    var expectedQuery = String.format(GET_ITEMS_BY_HOLDING_ID_QUERY, holdingsId);
    when(itemClient.getByQuery(expectedQuery, Integer.MAX_VALUE)).thenReturn(ItemCollection.builder()
      .items(List.of(Item.builder().discoverySuppress(true).build(),
        Item.builder().discoverySuppress(false).build()))
      .build());

    holdingsUpdateProcessor.updateAssociatedRecords(holdingsRecord, operation, notChanged);

    verify(itemClient, times(0)).updateItem(any(Item.class), anyString());

    if (notChanged) {
      verify(errorService).saveError(any(UUID.class), anyString(), eq(MSG_NO_CHANGE_REQUIRED), any(BulkOperationStep.class));
    } else {
      verify(errorService, times(0)).saveError(any(UUID.class), anyString(), anyString(), any(BulkOperationStep.class));
    }
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    SET_TO_TRUE   | false
    SET_TO_FALSE  | false
    SET_TO_TRUE   | true
    SET_TO_FALSE  | true
    """, delimiter = '|')
  void holdings_shouldUpdateAssociatedItemsDiscoverySuppress(UpdateActionType actionType, boolean notChanged) {
    var holdingsId = UUID.randomUUID().toString();
    var holdingsRecord = HoldingsRecord.builder()
      .id(holdingsId)
      .discoverySuppress(SET_TO_TRUE_INCLUDING_ITEMS.equals(actionType))
      .build();

    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .identifierType(IdentifierType.ID)
      .build();

    var rule = new BulkOperationRule().ruleDetails(new BulkOperationRuleRuleDetails()
      .option(UpdateOptionType.SUPPRESS_FROM_DISCOVERY)
      .actions(Collections.singletonList(new Action()
        .type(actionType)
        .parameters(Collections.singletonList(new Parameter()
          .key(APPLY_TO_ITEMS)
          .value("true"))))));
    when(ruleService.getRules(operationId)).thenReturn(new BulkOperationRuleCollection()
      .bulkOperationRules(Collections.singletonList(rule))
      .totalRecords(1));
    var expectedQuery = String.format(GET_ITEMS_BY_HOLDING_ID_QUERY, holdingsId);
    when(itemClient.getByQuery(expectedQuery, Integer.MAX_VALUE)).thenReturn(ItemCollection.builder()
      .items(List.of(Item.builder().id(UUID.randomUUID().toString()).discoverySuppress(true).build(),
        Item.builder().id(UUID.randomUUID().toString()).discoverySuppress(false).build()))
      .build());

    holdingsUpdateProcessor.updateAssociatedRecords(holdingsRecord, operation, notChanged);

    verify(itemClient, times(1)).updateItem(any(Item.class), anyString());

    if (notChanged) {
      var errorMessage = String.format("No change in value for holdings record required, associated %s item(s) have been updated.",
        SET_TO_TRUE_INCLUDING_ITEMS.equals(actionType) ? "unsuppressed" : "suppressed");
      verify(errorService).saveError(any(UUID.class), anyString(), eq(errorMessage), any(BulkOperationStep.class));
    } else {
      verify(errorService, times(0)).saveError(any(UUID.class), anyString(), anyString(), any(BulkOperationStep.class));
    }
  }


  @Test
  void shouldUpdateItem() {
    var item = new Item()
      .withId(UUID.randomUUID().toString())
      .withHoldingsRecordId(UUID.randomUUID().toString());

    itemUpdateProcessor.updateRecord(item);

    verify(itemClient).updateItem(item, item.getId());
  }

  @Test
  void shouldUpdateUser() {
    var user = new User()
        .withId(UUID.randomUUID().toString())
        .withPatronGroup(UUID.randomUUID().toString())
        .withUsername("sample_user");

    userUpdateProcessor.updateRecord(user);

    verify(userClient).updateUser(user, user.getId());
  }

  @Test
  void shouldUpdateInstance() {
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .title("Title")
      .build();

    instanceUpdateProcessor.updateRecord(instance);

    verify(instanceClient).updateInstance(instance, instance.getId());
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    SET_TO_TRUE   | true  | marc_id
    SET_TO_FALSE  | true  | marc_id
    SET_TO_TRUE   | false | marc_id
    SET_TO_FALSE  | false | marc_id
    SET_TO_TRUE   | true  | folio_id
    SET_TO_FALSE  | true  | folio_id
    SET_TO_TRUE   | false | folio_id
    SET_TO_FALSE  | false | folio_id
    """, delimiter = '|')
  void instance_testUpdateUnderlyingHoldingsDiscoverySuppress(UpdateActionType actionType, boolean applyToHoldings, String sourceId) {
    var instanceId = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(instanceId)
      .source("FOLIO")
      .discoverySuppress(SET_TO_TRUE.equals(actionType))
      .build();

    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .identifierType(IdentifierType.ID)
      .build();

    var rule = new BulkOperationRule().ruleDetails(new BulkOperationRuleRuleDetails()
      .option(UpdateOptionType.SUPPRESS_FROM_DISCOVERY)
      .actions(Collections.singletonList(new Action()
        .type(actionType)
        .parameters(Collections.singletonList(new Parameter()
          .key(APPLY_TO_HOLDINGS)
          .value(Boolean.toString(applyToHoldings)))))));
    when(ruleService.getRules(operationId)).thenReturn(new BulkOperationRuleCollection()
      .bulkOperationRules(Collections.singletonList(rule))
      .totalRecords(1));

    when(holdingsReferenceService.getSourceById("marc_id")).thenReturn(HoldingsRecordsSource.builder().name("MARC").build());
    when(holdingsReferenceService.getSourceById("folio_id")).thenReturn(HoldingsRecordsSource.builder().name("FOLIO").build());

    when(holdingsClient.getByQuery(String.format(GET_HOLDINGS_BY_INSTANCE_ID_QUERY, instanceId), Integer.MAX_VALUE))
      .thenReturn(HoldingsRecordCollection.builder()
        .holdingsRecords(List.of(HoldingsRecord.builder().id(UUID.randomUUID().toString()).sourceId(sourceId).discoverySuppress(true).build(),
          HoldingsRecord.builder().id(UUID.randomUUID().toString()).sourceId(sourceId).discoverySuppress(false).build()))
        .totalRecords(2)
        .build());

    instanceUpdateProcessor.updateAssociatedRecords(instance, operation, false);

    verify(holdingsClient, times("folio_id".equals(sourceId) && applyToHoldings ? 1 : 0)).updateHoldingsRecord(any(HoldingsRecord.class), anyString());
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    SET_TO_TRUE   | true  | marc_id
    SET_TO_FALSE  | true  | marc_id
    SET_TO_TRUE   | false | marc_id
    SET_TO_FALSE  | false | marc_id
    SET_TO_TRUE   | true  | folio_id
    SET_TO_FALSE  | true  | folio_id
    SET_TO_TRUE   | false | folio_id
    SET_TO_FALSE  | false | folio_id
    """, delimiter = '|')
  void instance_testUpdateUnderlyingItemsDiscoverySuppress(UpdateActionType actionType, boolean applyToItems, String sourceId) {
    var instanceId = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(instanceId)
      .source("FOLIO")
      .discoverySuppress(SET_TO_TRUE.equals(actionType))
      .build();

    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .identifierType(IdentifierType.ID)
      .build();

    var rule = new BulkOperationRule().ruleDetails(new BulkOperationRuleRuleDetails()
      .option(UpdateOptionType.SUPPRESS_FROM_DISCOVERY)
      .actions(Collections.singletonList(new Action()
        .type(actionType)
        .parameters(Collections.singletonList(new Parameter()
          .key(APPLY_TO_ITEMS)
          .value(Boolean.toString(applyToItems)))))));
    when(ruleService.getRules(operationId)).thenReturn(new BulkOperationRuleCollection()
      .bulkOperationRules(Collections.singletonList(rule))
      .totalRecords(1));

    when(holdingsReferenceService.getSourceById("marc_id")).thenReturn(HoldingsRecordsSource.builder().name("MARC").build());
    when(holdingsReferenceService.getSourceById("folio_id")).thenReturn(HoldingsRecordsSource.builder().name("FOLIO").build());

    var holdingsId = UUID.randomUUID().toString();
    when(holdingsClient.getByQuery(String.format(GET_HOLDINGS_BY_INSTANCE_ID_QUERY, instanceId), Integer.MAX_VALUE))
      .thenReturn(HoldingsRecordCollection.builder()
        .holdingsRecords(Collections.singletonList(HoldingsRecord.builder().id(holdingsId).sourceId(sourceId).discoverySuppress(true).build()))
        .totalRecords(1)
        .build());

    when(itemClient.getByQuery(String.format(GET_ITEMS_BY_HOLDING_ID_QUERY, holdingsId), Integer.MAX_VALUE))
      .thenReturn(ItemCollection.builder()
        .items(List.of(Item.builder().id(UUID.randomUUID().toString()).discoverySuppress(true).build(),
          Item.builder().id(UUID.randomUUID().toString()).discoverySuppress(false).build()))
        .totalRecords(2)
        .build());

    instanceUpdateProcessor.updateAssociatedRecords(instance, operation, false);

    verify(itemClient, times("folio_id".equals(sourceId) && applyToItems ? 1 : 0)).updateItem(any(Item.class), anyString());
  }
}
