package org.folio.bulkops.processor;

import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE_INCLUDING_ITEMS;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE_INCLUDING_ITEMS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemCollection;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.service.RuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

class UpdateProcessorTest extends BaseTest {

  @MockBean
  private RuleService ruleService;
  @SpyBean
  private ItemClient itemClient;
  @Autowired
  private HoldingsUpdateProcessor holdingsUpdateProcessor;
  @Autowired
  private ItemUpdateProcessor itemUpdateProcessor;
  @Autowired
  private UserUpdateProcessor userUpdateProcessor;

  @Test
  void shouldUpdateHoldingsRecord() {
    var holdingsRecord = new HoldingsRecord()
        .withId(UUID.randomUUID().toString())
        .withInstanceId(UUID.randomUUID().toString())
        .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()));

    when(ruleService.getRules(isA(UUID.class))).thenReturn(new BulkOperationRuleCollection());
    holdingsUpdateProcessor.updateRecord(holdingsRecord,"identifier", UUID.randomUUID());

    verify(holdingsClient).updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
  }

  @Test
  void shouldUpdateHoldingsRecordWithItemsByDiscoverySupressTrueValueWhenIncludingItemsAction() {
    var holdingsRecord = new HoldingsRecord()
      .withId(UUID.randomUUID().toString())
      .withInstanceId(UUID.randomUUID().toString())
      .withDiscoverySuppress(true)
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()));
    var item = new Item()
      .withId(UUID.randomUUID().toString())
      .withDiscoverySuppress(false);
    var itemsCollection = new ItemCollection().withItems(List.of(item));

    var ruleDetails = new BulkOperationRuleRuleDetails();
    ruleDetails.setOption(SUPPRESS_FROM_DISCOVERY);
    var action = new Action();
    action.setType(SET_TO_TRUE_INCLUDING_ITEMS);
    ruleDetails.setActions(List.of(action));
    var rule = new BulkOperationRule();
    rule.setRuleDetails(ruleDetails);
    var bulkOperationRuleCollection = new BulkOperationRuleCollection();
    bulkOperationRuleCollection.setBulkOperationRules(List.of(rule));

    when(ruleService.getRules(isA(UUID.class))).thenReturn(bulkOperationRuleCollection);
    when (itemClient.getByQuery(isA(String.class))).thenReturn(itemsCollection);

    holdingsUpdateProcessor.updateRecord(holdingsRecord,"identifier", UUID.randomUUID());

    verify(holdingsClient).updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
    verify(itemClient).updateItem(item, item.getId());

    assertTrue(item.getDiscoverySuppress());
  }

  @Test
  void shouldUpdateHoldingsRecordWithoutItemsByDiscoverySupressTrueValueWhenNotIncludingItemsAction() {
    var holdingsRecord = new HoldingsRecord()
      .withId(UUID.randomUUID().toString())
      .withInstanceId(UUID.randomUUID().toString())
      .withDiscoverySuppress(true)
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()));
    var item = new Item()
      .withId(UUID.randomUUID().toString())
      .withDiscoverySuppress(false);

    var ruleDetails = new BulkOperationRuleRuleDetails();
    ruleDetails.setOption(SUPPRESS_FROM_DISCOVERY);
    var action = new Action();
    action.setType(SET_TO_TRUE);
    ruleDetails.setActions(List.of(action));
    var rule = new BulkOperationRule();
    rule.setRuleDetails(ruleDetails);
    var bulkOperationRuleCollection = new BulkOperationRuleCollection();
    bulkOperationRuleCollection.setBulkOperationRules(List.of(rule));

    when(ruleService.getRules(isA(UUID.class))).thenReturn(bulkOperationRuleCollection);

    holdingsUpdateProcessor.updateRecord(holdingsRecord,"identifier", UUID.randomUUID());

    verify(holdingsClient).updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
    verify(itemClient, times(0)).getByQuery(isA(String.class));
    verify(itemClient, times(0)).updateItem(item, item.getId());

    assertFalse(item.getDiscoverySuppress());
  }

  @Test
  void shouldUpdateHoldingsRecordWithItemsByDiscoverySupressFalseValueWhenIncludingItemsAction() {
    var holdingsRecord = new HoldingsRecord()
      .withId(UUID.randomUUID().toString())
      .withInstanceId(UUID.randomUUID().toString())
      .withDiscoverySuppress(false)
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()));
    var item = new Item()
      .withId(UUID.randomUUID().toString())
      .withDiscoverySuppress(true);
    var itemsCollection = new ItemCollection().withItems(List.of(item));

    var ruleDetails = new BulkOperationRuleRuleDetails();
    ruleDetails.setOption(SUPPRESS_FROM_DISCOVERY);
    var action = new Action();
    action.setType(SET_TO_FALSE_INCLUDING_ITEMS);
    ruleDetails.setActions(List.of(action));
    var rule = new BulkOperationRule();
    rule.setRuleDetails(ruleDetails);
    var bulkOperationRuleCollection = new BulkOperationRuleCollection();
    bulkOperationRuleCollection.setBulkOperationRules(List.of(rule));

    when(ruleService.getRules(isA(UUID.class))).thenReturn(bulkOperationRuleCollection);
    when (itemClient.getByQuery(isA(String.class))).thenReturn(itemsCollection);

    holdingsUpdateProcessor.updateRecord(holdingsRecord,"identifier", UUID.randomUUID());

    verify(holdingsClient).updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
    verify(itemClient).updateItem(item, item.getId());

    assertFalse(item.getDiscoverySuppress());
  }

  @Test
  void shouldUpdateHoldingsRecordWithoutItemsByDiscoverySupressFalseValueWhenNotIncludingItemsAction() {
    var holdingsRecord = new HoldingsRecord()
      .withId(UUID.randomUUID().toString())
      .withInstanceId(UUID.randomUUID().toString())
      .withDiscoverySuppress(false)
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()));
    var item = new Item()
      .withId(UUID.randomUUID().toString())
      .withDiscoverySuppress(true);

    var ruleDetails = new BulkOperationRuleRuleDetails();
    ruleDetails.setOption(SUPPRESS_FROM_DISCOVERY);
    var action = new Action();
    action.setType(SET_TO_FALSE);
    ruleDetails.setActions(List.of(action));
    var rule = new BulkOperationRule();
    rule.setRuleDetails(ruleDetails);
    var bulkOperationRuleCollection = new BulkOperationRuleCollection();
    bulkOperationRuleCollection.setBulkOperationRules(List.of(rule));

    when(ruleService.getRules(isA(UUID.class))).thenReturn(bulkOperationRuleCollection);

    holdingsUpdateProcessor.updateRecord(holdingsRecord,"identifier", UUID.randomUUID());

    verify(holdingsClient).updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
    verify(itemClient, times(0)).getByQuery(isA(String.class));
    verify(itemClient, times(0)).updateItem(item, item.getId());

    assertTrue(item.getDiscoverySuppress());
  }

  @Test
  void shouldUpdateItem() {
    var item = new Item()
      .withId(UUID.randomUUID().toString())
      .withHoldingsRecordId(UUID.randomUUID().toString());

    itemUpdateProcessor.updateRecord(item, "identifier", UUID.randomUUID());

    verify(itemClient).updateItem(item, item.getId());
  }

  @Test
  void shouldUpdateUser() {
    var user = new User()
        .withId(UUID.randomUUID().toString())
        .withPatronGroup(UUID.randomUUID().toString())
        .withUsername("sample_user");

    userUpdateProcessor.updateRecord(user, "identifier",  UUID.randomUUID());

    verify(userClient).updateUser(user, user.getId());
  }
}
