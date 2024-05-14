package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.exception.OptimisticLockingException;
import org.folio.bulkops.processor.ItemUpdateProcessor;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.repository.BulkOperationRepository;
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
    var original = Item.builder()
      .id(UUID.randomUUID().toString())
      .barcode("barcode")
      .build();
    var modified = Item.builder()
      .id(UUID.randomUUID().toString())
      .barcode("barcode1")
      .version(1)
      .build();
    var operation = BulkOperation.builder()
      .id(UUID.randomUUID())
      .identifierType(IdentifierType.ID)
      .entityType(EntityType.ITEM)
      .build();

    assertThrows(OptimisticLockingException.class, () -> recordUpdateService.updateEntity(original, modified, operation));
  }
}
