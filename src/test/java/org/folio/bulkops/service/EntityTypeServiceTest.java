package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.bulkops.client.EntityTypeClient;
import org.folio.querytool.domain.dto.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
class EntityTypeServiceTest {
  @Mock
  private EntityTypeClient entityTypeClient;
  @InjectMocks
  private EntityTypeService entityTypeService;

  private MockitoSession mockito;

  @BeforeEach
  void setUp() {
    mockito = Mockito.mockitoSession()
        .initMocks(this)
        .strictness(Strictness.STRICT_STUBS)
        .startMocking();
  }

  @AfterEach
  void tearDown() {
    mockito.finishMocking(); // обязательно
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
    7663cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_user_details | Users | USER
    3463cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_item_details | Items | ITEM
    2363cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_holdings_record | Holdings | HOLDINGS_RECORD
    1163cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_instances | Instances | INSTANCE
    5363cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_item_details | ItemFr | ITEM
    7863cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_user_details | UserFr | USER
    8963cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_holdings_record | HoldFr | HOLDINGS_RECORD
    9063cc37-ed7c-4905-9ef1-a23ac4d4f9df | composite_instances | InstFr | INSTANCE
    """, delimiter = '|')
  void testGetEntityType(UUID entityTypeId, String name, String alias, org.folio.bulkops.domain.dto.EntityType expectedType) {
    when(entityTypeClient.getEntityType(entityTypeId)).thenReturn(new EntityType().name(name).labelAlias(alias));

    var actualType = entityTypeService.getEntityTypeById(entityTypeId);

    assertEquals(expectedType, actualType);
  }
}
