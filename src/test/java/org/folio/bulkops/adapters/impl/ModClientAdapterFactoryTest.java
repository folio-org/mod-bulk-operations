package org.folio.bulkops.adapters.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.bulkops.adapters.ModClientAdapterFactory;
import org.folio.bulkops.adapters.impl.holdings.HoldingModClientAdapter;
import org.folio.bulkops.adapters.impl.items.ItemModClientAdapter;
import org.folio.bulkops.adapters.impl.users.UserModClientAdapter;
import org.folio.bulkops.domain.dto.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModClientAdapterFactoryTest {
  @Mock
  private UserModClientAdapter userModClientAdapter;
  @Mock
  private ItemModClientAdapter itemModClientAdapter;
  @Mock
  private HoldingModClientAdapter holdingModClientAdapter;
  @InjectMocks
  private ModClientAdapterFactory modClientAdapterFactory;

  @Test
  void getModClientAdapterTest() {
    var adapter = modClientAdapterFactory.getModClientAdapter(EntityType.USER);
    assertTrue(adapter instanceof UserModClientAdapter);
    adapter = modClientAdapterFactory.getModClientAdapter(EntityType.HOLDING);
    assertTrue(adapter instanceof HoldingModClientAdapter);
    adapter = modClientAdapterFactory.getModClientAdapter(EntityType.ITEM);
    assertTrue(adapter instanceof ItemModClientAdapter);
  }
}
