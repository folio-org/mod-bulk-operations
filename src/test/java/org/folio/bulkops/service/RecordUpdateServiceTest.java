package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.MSG_ERROR_OPTIMISTIC_LOCKING;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.processor.ItemUpdateProcessor;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

class RecordUpdateServiceTest extends BaseTest {
  @Autowired
  private RecordUpdateService recordUpdateService;
  @MockBean
  private BulkOperationExecutionContentRepository executionContentRepository;
  @MockBean
  private BulkOperationRepository bulkOperationRepository;
  @SpyBean
  private ItemUpdateProcessor itemUpdateProcessor;
  @SpyBean
  private ErrorService errorService;

  @Test
  void testUpdateNonEqualEntity() {
    var original = Item.builder()
      .id(UUID.randomUUID().toString())
      .barcode("barcode")
      .build();
    var modified = original.withCallNumber("call number");
    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.ID)
      .entityType(EntityType.ITEM)
      .build();

    var result = recordUpdateService.updateEntity(original, modified, operation);

    assertEquals(modified, result);
    verify(itemUpdateProcessor).updateRecord(any(Item.class));
    verify(errorService, times(0)).saveError(any(UUID.class), anyString(), anyString());
  }

  @Test
  void testUpdateNonModifiedEntity() {
    var original = Item.builder()
      .id(UUID.randomUUID().toString())
      .barcode("barcode")
      .build();
    var modified = original;
    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.ID)
      .entityType(EntityType.ITEM)
      .build();

    var result = recordUpdateService.updateEntity(original, modified, operation);

    assertEquals(original, result);
    verify(itemUpdateProcessor, times(0)).updateRecord(any(Item.class));
    verify(errorService).saveError(operation.getId(), original.getIdentifier(IdentifierType.ID), MSG_NO_CHANGE_REQUIRED);
  }

  @Test
  void testUpdateModifiedEntityWithOptimisticLockingError() {
    var feignException = FeignException.errorStatus("", Response.builder().status(409)
      .reason("optimistic locking")
      .request(Request.create(Request.HttpMethod.GET, "", Map.of(), new byte[]{}, Charset.defaultCharset(), null))
      .build());
    doThrow(feignException).when(itemClient).updateItem(any(Item.class), any(String.class));
    when(holdingsClient.getHoldingById("cb475fa9-aa07-4bbf-8382-b0b1426f9a20")).thenReturn(HoldingsRecord.builder().instanceId("f3e3bd0f-1d95-4f25-9df1-7eb39a2957e3").build());
    var original = Item.builder()
      .id("1f5e22ed-92ed-4c65-a603-2a5cb4c6052e")
      .barcode("barcode")
      .holdingsRecordId("cb475fa9-aa07-4bbf-8382-b0b1426f9a20")
      .build();
    var modified = Item.builder()
      .id("1f5e22ed-92ed-4c65-a603-2a5cb4c6052e")
      .barcode("barcode1")
      .holdingsRecordId("cb475fa9-aa07-4bbf-8382-b0b1426f9a20")
      .version(1)
      .build();
    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.ID)
      .entityType(EntityType.ITEM)
      .build();

    try {
      recordUpdateService.updateEntity(original, modified, operation);
    } catch (Exception e) {
      var msg = e.getMessage();
      assertThat(msg, Matchers.startsWith(MSG_ERROR_OPTIMISTIC_LOCKING));
      assertThat(msg.split(MSG_ERROR_OPTIMISTIC_LOCKING)[1].trim(), Matchers.equalTo("/inventory/view/f3e3bd0f-1d95-4f25-9df1-7eb39a2957e3/cb475fa9-aa07-4bbf-8382-b0b1426f9a20/1f5e22ed-92ed-4c65-a603-2a5cb4c6052e"));
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
    var modified = Instance.builder()
      .id(UUID.randomUUID().toString())
      .version(1)
      .build();
    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.ID)
      .entityType(EntityType.INSTANCE)
      .build();

    assertThrows(FeignException.class, () -> recordUpdateService.updateEntity(original, modified, operation));
  }
}
