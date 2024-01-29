package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.folio.bulkops.client.EntityTypeClient;
import org.folio.querytool.domain.dto.EntityType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EntityTypeServiceTest {
  @Mock
  private EntityTypeClient entityTypeClient;
  @InjectMocks
  private EntityTypeService entityTypeService;

  @ParameterizedTest
  @CsvSource(textBlock = """
    ae6a4972-0e0d-4069-9936-99ba6e91658d  | Users | USER
    a5f253dc-3215-46bb-9714-2b26d8b3b613  | Items | ITEM
    """, delimiter = '|')
  void testGetEntityType(UUID entityTypeId, String alias, org.folio.bulkops.domain.dto.EntityType expectedType) {
    when(entityTypeClient.getEntityType(entityTypeId)).thenReturn(new EntityType().labelAlias(alias));

    var actualType = entityTypeService.getEntityTypeById(entityTypeId);

    assertEquals(expectedType, actualType);
  }
}
