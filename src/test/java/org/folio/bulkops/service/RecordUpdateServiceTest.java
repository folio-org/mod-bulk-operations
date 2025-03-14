package org.folio.bulkops.service;

import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.EntityType.INSTANCE;
import static org.folio.bulkops.util.Constants.MSG_NO_ADMINISTRATIVE_CHANGE_REQUIRED;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.OptimisticLockingException;
import org.folio.bulkops.processor.folio.ItemUpdateProcessor;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.bulkops.util.EntityPathResolver;
import org.folio.bulkops.util.Utils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class RecordUpdateServiceTest extends BaseTest {
  @Autowired
  private RecordUpdateService recordUpdateService;
  @Autowired
  private EntityPathResolver entityPathResolver;
   @MockitoBean
  private BulkOperationExecutionContentRepository executionContentRepository;
   @MockitoBean
  private ConsortiaService consortiaService;
   @MockitoBean
  private BulkOperationRepository bulkOperationRepository;
   @MockitoBean
  private PermissionsValidator permissionsValidator;
   @MockitoBean
  private RuleService ruleService;
  @MockitoSpyBean
  private ItemUpdateProcessor itemUpdateProcessor;
  @MockitoSpyBean
  private ErrorService errorService;
  @MockitoSpyBean
  private FolioExecutionContext folioExecutionContext;

  @Test
  void testUpdateNonEqualEntity() {
    var original = Item.builder()
      .id(UUID.randomUUID().toString())
      .barcode("barcode")
      .build();
    var extendedOriginalItem = ExtendedItem.builder().entity(original).build();
    var modified = original.withItemLevelCallNumber("call number");
    var extendedModifiedItem = ExtendedItem.builder().entity(modified).build();
    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.ID)
      .entityType(EntityType.ITEM)
      .build();

    when(consortiaService.isTenantCentral(any())).thenReturn(false);
    doNothing().when(permissionsValidator).checkIfBulkEditWritePermissionExists(anyString(), any(), anyString());

    var result = recordUpdateService.updateEntity(extendedOriginalItem, extendedModifiedItem, operation);

    assertEquals(extendedModifiedItem, result);
    verify(itemUpdateProcessor).updateRecord(any(ExtendedItem.class));
    verify(errorService, times(0)).saveError(any(UUID.class), anyString(), anyString(), eq(ErrorType.WARNING));
  }

  @Test
  void testUpdateNonModifiedEntity() {
    var original = Item.builder()
      .id(UUID.randomUUID().toString())
      .barcode("barcode")
      .build();
    var extendedOriginalItem = ExtendedItem.builder().entity(original).build();
    var modified = original;
    var extendedModifiedItem = ExtendedItem.builder().entity(modified).build();
    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.ID)
      .entityType(EntityType.ITEM)
      .build();

    var result = recordUpdateService.updateEntity(extendedOriginalItem, extendedModifiedItem, operation);

    assertEquals(extendedOriginalItem, result);
    verify(itemUpdateProcessor, times(0)).updateRecord(any(ExtendedItem.class));
    verify(errorService).saveError(operation.getId(), original.getIdentifier(IdentifierType.ID), MSG_NO_CHANGE_REQUIRED, ErrorType.WARNING);
  }

  @ParameterizedTest
  @EnumSource(value = EntityType.class, names = {"INSTANCE", "INSTANCE_MARC"}, mode = EnumSource.Mode.INCLUDE)
  void shouldChangeErrorMessageForNonModifiedMarcInstances(EntityType entityType) {
    var original = Instance.builder()
      .id(UUID.randomUUID().toString())
      .hrid("hrid")
      .discoverySuppress(false)
      .build();
    var extendedOriginalInstance = ExtendedInstance.builder().entity(original).build();
    var extendedModifiedInstance = ExtendedInstance.builder().entity(original).build();
    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.HRID)
      .entityType(entityType)
      .build();

    when(ruleService.getRules(operation.getId())).thenReturn(new BulkOperationRuleCollection()
      .bulkOperationRules(Collections.emptyList())
      .totalRecords(0));

    var result = recordUpdateService.updateEntity(extendedOriginalInstance, extendedModifiedInstance, operation);

    assertEquals(extendedOriginalInstance, result);
    var expectedErrorMessage = INSTANCE.equals(entityType) ? MSG_NO_CHANGE_REQUIRED : MSG_NO_ADMINISTRATIVE_CHANGE_REQUIRED;
    verify(errorService).saveError(operation.getId(), original.getIdentifier(IdentifierType.HRID), expectedErrorMessage, ErrorType.WARNING);
  }

  @ParameterizedTest
  @ValueSource(strings = {"[409 Conflict] during [PUT] to [http://inventory/items/23f2c8e1-bd5d-4f27-9398-a688c998808a] [ItemClient#updateItem(Item,String)]: [ERROR: Cannot update record 23f2c8e1-bd5d-4f27-9398-a688c998808a because it has been changed (optimistic locking): Stored _version is 13, _version of request is 12 (23F09)]",
    "[409 Conflict] during [PUT] to [http://inventory/items/23f2c8e1-bd5d-4f27-9398-a688c998808a] [ItemClient#updateItem(Item,String)]: [ERROR: Cannot update record 23f2c8e1-bd5d-4f27-9398-a688c998808a because it has been changed (optimistic locking): S"})
  void testUpdateModifiedEntityWithOptimisticLockingError(String responseErrorMessage) {
    var feignException = FeignException.errorStatus("", Response.builder().status(409)
      .reason("null".equals(responseErrorMessage) ? null : responseErrorMessage)
      .request(Request.create(Request.HttpMethod.PUT, "", Map.of(), new byte[]{}, Charset.defaultCharset(), null))
      .build());
    var tenantId = "diku";
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of(tenantId));

    when(consortiaService.isTenantCentral(any())).thenReturn(false);
    doNothing().when(permissionsValidator).checkIfBulkEditWritePermissionExists(anyString(), any(), anyString());
    doThrow(feignException).when(itemClient).updateItem(any(Item.class), any(String.class));
    when(holdingsClient.getHoldingById("cb475fa9-aa07-4bbf-8382-b0b1426f9a20")).thenReturn(HoldingsRecord.builder().instanceId("f3e3bd0f-1d95-4f25-9df1-7eb39a2957e3").build());
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);

    var original = Item.builder()
      .id("1f5e22ed-92ed-4c65-a603-2a5cb4c6052e")
      .barcode("barcode")
      .holdingsRecordId("cb475fa9-aa07-4bbf-8382-b0b1426f9a20")
      .version(2)
      .build();
    var extendedOriginalItem = ExtendedItem.builder().entity(original).tenantId(tenantId).build();
    var modified = Item.builder()
      .id("1f5e22ed-92ed-4c65-a603-2a5cb4c6052e")
      .barcode("barcode1")
      .holdingsRecordId("cb475fa9-aa07-4bbf-8382-b0b1426f9a20")
      .version(1)
      .build();
    var extendedModifiedItem = ExtendedItem.builder().entity(modified).build();
    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.ID)
      .entityType(EntityType.ITEM)
      .build();


    try {
      recordUpdateService.updateEntity(extendedOriginalItem, extendedModifiedItem, operation);
    } catch (OptimisticLockingException e) {
      var expectedUiErrorMessage = Utils.getMessageFromFeignException(feignException);
      var link = entityPathResolver.resolve(operation.getEntityType(), extendedOriginalItem);
      var expectedCsvErrorMessage = format("%s %s", expectedUiErrorMessage, link);
      assertThat(e.getCsvErrorMessage(), Matchers.equalTo(expectedCsvErrorMessage));
      assertThat(e.getUiErrorMessage(), Matchers.equalTo(expectedUiErrorMessage));
    }
  }

  @Test
  void testUpdateModifiedEntityWithOtherError() {
    var feignException = FeignException.errorStatus("", Response.builder().status(409)
      .reason("other reason")
      .request(Request.create(Request.HttpMethod.GET, "", Map.of(), new byte[]{}, Charset.defaultCharset(), null))
      .build());
    doThrow(feignException).when(instanceClient).updateInstance(any(Instance.class), any(String.class));
    var original = Instance.builder()
      .id(UUID.randomUUID().toString())
      .build();
    var extendedOriginal = ExtendedInstance.builder().entity(original).build();
    var modified = Instance.builder()
      .id(UUID.randomUUID().toString())
      .version(1)
      .build();
    var extendedModified = ExtendedInstance.builder().entity(modified).build();
    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.ID)
      .entityType(INSTANCE)
      .build();

    assertThrows(FeignException.class, () -> recordUpdateService.updateEntity(extendedOriginal, extendedModified, operation));
  }
}
