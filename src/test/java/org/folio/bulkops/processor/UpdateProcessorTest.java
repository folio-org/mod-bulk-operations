package org.folio.bulkops.processor;

import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.SearchConsortium;
import org.folio.bulkops.domain.bean.ConsortiumHolding;
import org.folio.bulkops.domain.bean.ConsortiumHoldingCollection;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.ExtendedItem;
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
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.RuleDetails;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.processor.folio.FolioInstanceUpdateProcessor;
import org.folio.bulkops.processor.folio.HoldingsUpdateProcessor;
import org.folio.bulkops.processor.folio.ItemUpdateProcessor;
import org.folio.bulkops.processor.folio.UserUpdateProcessor;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.RuleService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class UpdateProcessorTest extends BaseTest {

   @MockitoBean
  private RuleService ruleService;
  @MockitoSpyBean
  private ItemClient itemClient;
  @MockitoSpyBean
  private ErrorService errorService;
   @MockitoBean
  private BulkOperationExecutionContentRepository executionContentRepository;
   @MockitoBean
  private BulkOperationRepository bulkOperationRepository;
  @Autowired
  private HoldingsUpdateProcessor holdingsUpdateProcessor;
  @Autowired
  private ItemUpdateProcessor itemUpdateProcessor;
  @Autowired
  private UserUpdateProcessor userUpdateProcessor;
  @Autowired
  private FolioInstanceUpdateProcessor folioInstanceUpdateProcessor;
  @Autowired
  private FolioModuleMetadata folioModuleMetadata;
  @MockitoSpyBean
  private FolioExecutionContext folioExecutionContext;
   @MockitoBean
  private HoldingsReferenceService holdingsReferenceService;
   @MockitoBean
  private ConsortiaService consortiaService;
   @MockitoBean
  private SearchConsortium searchConsortium;
   @MockitoBean
  private PermissionsValidator permissionsValidator;

  @Test
  void shouldUpdateHoldingsRecord() {
    var holdingsRecord = new HoldingsRecord()
        .withId(UUID.randomUUID().toString())
        .withInstanceId(UUID.randomUUID().toString());
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holdingsRecord).build();

    doNothing().when(permissionsValidator).checkIfBulkEditWritePermissionExists(anyString(), any(), anyString());
    when(ruleService.getRules(isA(UUID.class))).thenReturn(new BulkOperationRuleCollection());

    holdingsUpdateProcessor.updateRecord(extendedHoldingsRecord);

    verify(holdingsStorageClient).updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
  }

  @Test
  void shouldUpdateHoldingsRecordWithTenant() {
    var holdingsRecord = new HoldingsRecord()
      .withId(UUID.randomUUID().toString())
      .withInstanceId(UUID.randomUUID().toString());
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().tenantId("tenantId").entity(holdingsRecord).build();
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("diku"));

    doNothing().when(permissionsValidator).checkIfBulkEditWritePermissionExists(eq("tenantId"), eq(EntityType.HOLDINGS_RECORD), anyString());
    when(ruleService.getRules(isA(UUID.class))).thenReturn(new BulkOperationRuleCollection());
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);

    holdingsUpdateProcessor.updateRecord(extendedHoldingsRecord);

    verify(holdingsStorageClient).updateHoldingsRecord(holdingsRecord, holdingsRecord.getId());
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
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holdingsRecord).build();

    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .identifierType(IdentifierType.ID)
      .build();

    var rule = new BulkOperationRule().ruleDetails(new RuleDetails()
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

    holdingsUpdateProcessor.updateAssociatedRecords(extendedHoldingsRecord, operation, notChanged);

    verify(itemClient, times(0)).updateItem(any(Item.class), anyString());

    if (notChanged) {
      verify(errorService).saveError(any(UUID.class), anyString(), eq(MSG_NO_CHANGE_REQUIRED), eq(ErrorType.WARNING));
    } else {
      verify(errorService, times(0)).saveError(any(UUID.class), anyString(), anyString(), eq(ErrorType.ERROR));
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
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holdingsRecord).build();

    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .identifierType(IdentifierType.ID)
      .build();

    var rule = new BulkOperationRule().ruleDetails(new RuleDetails()
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

    holdingsUpdateProcessor.updateAssociatedRecords(extendedHoldingsRecord, operation, notChanged);

    verify(itemClient, times(1)).updateItem(any(Item.class), anyString());

    if (notChanged) {
      var errorMessage = String.format("No change in value for holdings record required, associated %s item(s) have been updated.",
        SET_TO_TRUE_INCLUDING_ITEMS.equals(actionType) ? "unsuppressed" : "suppressed");
      verify(errorService).saveError(any(UUID.class), anyString(), eq(errorMessage), eq(ErrorType.WARNING));
    } else {
      verify(errorService, times(0)).saveError(any(UUID.class), anyString(), anyString(), eq(ErrorType.ERROR));
    }
  }


  @Test
  void shouldUpdateItem() {
    var item = new Item()
      .withId(UUID.randomUUID().toString())
      .withHoldingsRecordId(UUID.randomUUID().toString());
    var extendedItem = ExtendedItem.builder().entity(item).build();

    doNothing().when(permissionsValidator).checkIfBulkEditWritePermissionExists(anyString(), eq(EntityType.ITEM), anyString());
    itemUpdateProcessor.updateRecord(extendedItem);

    verify(itemClient).updateItem(item, item.getId());
  }

  @Test
  void shouldUpdateItemRecordWithTenant() {
    var item = new Item()
      .withId(UUID.randomUUID().toString())
      .withHoldingsRecordId(UUID.randomUUID().toString());
    var extendedItem = ExtendedItem.builder().tenantId("tenantId").entity(item).build();
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("diku"));

    doNothing().when(permissionsValidator).checkIfBulkEditWritePermissionExists(eq("tenantId"), eq(EntityType.ITEM), anyString());
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);

    itemUpdateProcessor.updateRecord(extendedItem);

    verify(itemClient).updateItem(item, item.getId());
  }

  @Test
  void shouldUpdateUser() {
    var user = new User()
        .withId(UUID.randomUUID().toString())
        .withPatronGroup(UUID.randomUUID().toString())
        .withUsername("sample_user");

    doNothing().when(permissionsValidator).checkIfBulkEditWritePermissionExists(anyString(), eq(EntityType.USER), anyString());

    userUpdateProcessor.updateRecord(user);

    verify(userClient).updateUser(user, user.getId());
  }

  @Test
  void shouldUpdateInstance() {
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .title("Title")
      .build();
    var extendedInstance = ExtendedInstance.builder().entity(instance).build();
    doNothing().when(permissionsValidator).checkIfBulkEditWritePermissionExists(anyString(), eq(EntityType.INSTANCE), anyString());

    folioInstanceUpdateProcessor.updateRecord(extendedInstance);

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
    var extendedInstance = ExtendedInstance.builder().entity(instance).build();
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .identifierType(IdentifierType.ID)
      .build();

    var rule = new BulkOperationRule().ruleDetails(new RuleDetails()
      .option(UpdateOptionType.SUPPRESS_FROM_DISCOVERY)
      .actions(Collections.singletonList(new Action()
        .type(actionType)
        .parameters(Collections.singletonList(new Parameter()
          .key(APPLY_TO_HOLDINGS)
          .value(Boolean.toString(applyToHoldings)))))));

    when(consortiaService.isTenantCentral(isA(String.class))).thenReturn(false);
    when(ruleService.getRules(operationId)).thenReturn(new BulkOperationRuleCollection()
      .bulkOperationRules(Collections.singletonList(rule))
      .totalRecords(1));

    when(holdingsReferenceService.getSourceById("marc_id")).thenReturn(HoldingsRecordsSource.builder().name("MARC").build());
    when(holdingsReferenceService.getSourceById("folio_id")).thenReturn(HoldingsRecordsSource.builder().name("FOLIO").build());

    when(
        holdingsStorageClient.getByQuery(String.format(GET_HOLDINGS_BY_INSTANCE_ID_QUERY, instanceId), Integer.MAX_VALUE))
      .thenReturn(HoldingsRecordCollection.builder()
        .holdingsRecords(List.of(HoldingsRecord.builder().id(UUID.randomUUID().toString()).sourceId(sourceId).discoverySuppress(true).build(),
          HoldingsRecord.builder().id(UUID.randomUUID().toString()).sourceId(sourceId).discoverySuppress(false).build()))
        .totalRecords(2)
        .build());

    folioInstanceUpdateProcessor.updateAssociatedRecords(extendedInstance, operation, false);

    verify(holdingsStorageClient, times("folio_id".equals(sourceId) && applyToHoldings ? 1 : 0)).updateHoldingsRecord(any(HoldingsRecord.class), anyString());
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
    var extendedInstance = ExtendedInstance.builder().entity(instance).build();
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .identifierType(IdentifierType.ID)
      .build();

    when(consortiaService.isTenantCentral(isA(String.class))).thenReturn(false);
    var rule = new BulkOperationRule().ruleDetails(new RuleDetails()
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
    when(
        holdingsStorageClient.getByQuery(String.format(GET_HOLDINGS_BY_INSTANCE_ID_QUERY, instanceId), Integer.MAX_VALUE))
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

    folioInstanceUpdateProcessor.updateAssociatedRecords(extendedInstance, operation, false);

    verify(itemClient, times("folio_id".equals(sourceId) && applyToItems ? 1 : 0)).updateItem(any(Item.class), anyString());
  }

  @Test
  void instance_testUpdateUnderlyingHoldingsAndItemsDiscoverySuppressForMemberTenant() {
    var userId = UUID.randomUUID().toString();
    var user = User.builder().id(userId).username("username").build();
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("diku"));
    var applyToHoldings = true;
    var sourceId = "folio_id";
    var instanceId = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(instanceId)
      .source("FOLIO")
      .discoverySuppress(true)
      .build();
    var extendedInstance = ExtendedInstance.builder().entity(instance).build();
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .identifierType(IdentifierType.ID)
      .build();
    var rule = new BulkOperationRule().ruleDetails(new RuleDetails()
      .option(UpdateOptionType.SUPPRESS_FROM_DISCOVERY)
      .actions(Collections.singletonList(new Action()
        .type(SET_TO_FALSE)
        .parameters(List.of(new Parameter()
            .key(APPLY_TO_HOLDINGS)
            .value(Boolean.toString(applyToHoldings)),
          new Parameter()
            .key(APPLY_TO_ITEMS)
            .value(Boolean.toString(applyToHoldings))
        )))));
    var item = Item.builder().id(UUID.randomUUID().toString()).discoverySuppress(false).build();
    var itemCollection = ItemCollection.builder()
      .items(List.of(item))
      .totalRecords(1)
      .build();
    var holdingRecord = HoldingsRecord.builder().id(UUID.randomUUID().toString())
      .sourceId(sourceId).discoverySuppress(false).build();
    var consortiumHolding = new ConsortiumHolding(holdingRecord.getId(), "memberTenant", instanceId);
    var consortiumHolding2 = new ConsortiumHolding(UUID.randomUUID().toString(), "memberTenant2", instanceId);
    var consortiumHoldingCollection = new ConsortiumHoldingCollection();
    consortiumHoldingCollection.setHoldings(List.of(consortiumHolding, consortiumHolding2));
    var affiliatedTenants = List.of("memberTenant");

    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.fromString(userId));
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(userClient.getUserById(userId)).thenReturn(user);
    when(consortiaService.isTenantCentral("diku")).thenReturn(true);
    when(consortiaService.getAffiliatedTenants(any(), any())).thenReturn(affiliatedTenants);
    when(searchConsortium.getHoldingsById(UUID.fromString(instanceId))).thenReturn(consortiumHoldingCollection);
    when(ruleService.getRules(operationId)).thenReturn(new BulkOperationRuleCollection()
      .bulkOperationRules(Collections.singletonList(rule))
      .totalRecords(1));
    when(holdingsReferenceService.getSourceById("folio_id")).thenReturn(HoldingsRecordsSource.builder().name("FOLIO").build());
    when(
        holdingsStorageClient.getByQuery(String.format(GET_HOLDINGS_BY_INSTANCE_ID_QUERY, instanceId), Integer.MAX_VALUE))
      .thenReturn(HoldingsRecordCollection.builder()
        .holdingsRecords(List.of(holdingRecord))
        .totalRecords(1)
        .build());
    when(itemClient.getByQuery(String.format(GET_ITEMS_BY_HOLDING_ID_QUERY, holdingRecord.getId()), Integer.MAX_VALUE))
      .thenReturn(itemCollection);

    folioInstanceUpdateProcessor.updateAssociatedRecords(extendedInstance, operation, false);
    verify(errorService).saveError(eq(operationId), eq(instanceId), anyString(), eq(ErrorType.ERROR));
    verify(holdingsStorageClient).updateHoldingsRecord(any(HoldingsRecord.class), eq(holdingRecord.getId()));
    verify(itemClient).updateItem(any(Item.class), eq(item.getId()));
  }

  @Test
  void testSaveWarning_WhenInstanceNotChanged() {
    var instanceId = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(instanceId)
      .source("FOLIO")
      .discoverySuppress(true)
      .build();
    var extendedInstance = ExtendedInstance.builder().entity(instance).build();
    var operationId = UUID.randomUUID();
    var operation = BulkOperation.builder()
      .id(operationId)
      .identifierType(IdentifierType.ID)
      .build();

    var rule = new BulkOperationRule().ruleDetails(new RuleDetails()
      .option(UpdateOptionType.SUPPRESS_FROM_DISCOVERY)
      .actions(Collections.singletonList(new Action()
        .type(SET_TO_TRUE)
        .parameters(Collections.singletonList(new Parameter()
          .key(APPLY_TO_HOLDINGS)
          .value(Boolean.toString(false)))))));

    when(consortiaService.isTenantCentral(isA(String.class))).thenReturn(false);
    when(ruleService.getRules(operationId)).thenReturn(new BulkOperationRuleCollection()
        .bulkOperationRules(Collections.singletonList(rule)));

    folioInstanceUpdateProcessor.updateAssociatedRecords(extendedInstance, operation, true);

    verify(errorService).saveError(eq(operationId), eq(instanceId), anyString(), eq(ErrorType.WARNING));
  }
}
