package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.bulkops.client.EntityTypeClient;
import org.folio.querytool.domain.dto.EntityType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntityTypeServiceTest {
  @Mock
  private EntityTypeClient entityTypeClient;
  @InjectMocks
  private EntityTypeService entityTypeService;

  @ParameterizedTest
  @CsvSource(textBlock = """
    ae6a4972-0e0d-4069-9936-99ba6e91658d | Users | USER
    a5f253dc-3215-46bb-9714-2b26d8b3b613 | Items | ITEM
    65f253dc-3215-46bb-9714-2b26d8b3b613 | Holdings | HOLDINGS_RECORD
    75f253dc-3215-46bb-9714-2b26d8b3b613 | Instances | INSTANCE
    d0213d22-32cf-490f-9196-d81c3c66e53f | ItemFr | ITEM
    ddc93926-d15a-4a45-9d9c-93eadc3d9bbf | UserFr | USER
    8418e512-feac-4a6a-a56d-9006aab31e33 | HoldFr | HOLDINGS_RECORD
    6b08439b-4f8e-4468-8046-ea620f5cfb74 | InstFr | INSTANCE
    """, delimiter = '|')
  void testGetEntityType(UUID entityTypeId, String alias, org.folio.bulkops.domain.dto.EntityType expectedType) {
    when(entityTypeClient.getEntityType(entityTypeId)).thenReturn(new EntityType().labelAlias(alias)
            .id(entityTypeId.toString()));

    var actualType = entityTypeService.getEntityTypeById(entityTypeId);

    assertEquals(expectedType, actualType);
  }
}
