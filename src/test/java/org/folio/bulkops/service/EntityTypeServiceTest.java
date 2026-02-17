package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.bulkops.client.EntityTypeClient;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.querytool.domain.dto.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntityTypeServiceTest {
  @Mock private EntityTypeClient entityTypeClient;
  @InjectMocks private EntityTypeService entityTypeService;

  @ParameterizedTest
  @CsvSource(
      textBlock =
          """
          7663cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_user_details | Users | USER
          3463cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_item_details | Items | ITEM
          2363cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_holdings_record | Holdings | HOLDINGS_RECORD
          1163cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_instances | Instances | INSTANCE
          5363cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_item_details | ItemFr | ITEM
          7863cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_user_details | UserFr | USER
          8963cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_holdings_record | HoldFr | HOLDINGS_RECORD
          9063cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_instances | InstFr | INSTANCE
          """,
      delimiter = '|')
  void testGetEntityType(
      UUID entityTypeId,
      String name,
      String alias,
      org.folio.bulkops.domain.dto.EntityType expectedType) {
    when(entityTypeClient.getEntityType(entityTypeId))
        .thenReturn(new EntityType().name(name).labelAlias(alias));

    var actualType = entityTypeService.getBulkOpsEntityTypeByFqmEntityTypeId(entityTypeId);

    assertEquals(expectedType, actualType);
  }

  @Test
  void getBulkOpsEntityTypeByFqmEntityTypeIdThrowsNotFoundExceptionOnFeignException() {
    UUID entityTypeId = UUID.randomUUID();
    when(entityTypeClient.getEntityType(entityTypeId))
        .thenThrow(new RuntimeException("Service unavailable"));

    assertThrows(
        RuntimeException.class,
        () -> entityTypeService.getBulkOpsEntityTypeByFqmEntityTypeId(entityTypeId));
  }

  @Test
  void getBulkOpsEntityTypeByFqmEntityTypeIdRethrowsNotFoundExceptionWhenCaught() {
    UUID entityTypeId = UUID.randomUUID();
    when(entityTypeClient.getEntityType(entityTypeId))
        .thenThrow(new NotFoundException("Entity type not found"));

    assertThrows(
        NotFoundException.class,
        () -> entityTypeService.getBulkOpsEntityTypeByFqmEntityTypeId(entityTypeId));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenEntityTypeNameIsNotSupported() {

    UUID entityTypeId = UUID.randomUUID();
    EntityType entityTypeDto = mock(EntityType.class);

    when(entityTypeClient.getEntityType(entityTypeId)).thenReturn(entityTypeDto);
    when(entityTypeDto.getName()).thenReturn("unsupported_entity_type");

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> entityTypeService.getBulkOpsEntityTypeByFqmEntityTypeId(entityTypeId));
    assertEquals(
        String.format("Entity type with name=%s is not supported", "unsupported_entity_type"),
        exception.getMessage());
  }
}
