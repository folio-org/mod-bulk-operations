package org.folio.bulkops.adapters.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.adapters.HoldingUnifiedTableHeaderBuilder;
import org.folio.bulkops.adapters.ItemUnifiedTableHeaderBuilder;
import org.folio.bulkops.adapters.ModClientAdapterFactory;
import org.folio.bulkops.adapters.UserUnifiedTableHeaderBuilder;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UnifiedTableHeaderBuilderAdapterFactoryTest extends BaseTest {
  @Autowired
  private ModClientAdapterFactory modClientAdapterFactory;
  @Test
  void getModClientAdapterTest() {
    assertEquals(UserUnifiedTableHeaderBuilder.class, modClientAdapterFactory.getModClientAdapter(User.class)
      .getClass());
    assertEquals(HoldingUnifiedTableHeaderBuilder.class, modClientAdapterFactory.getModClientAdapter(HoldingsRecord.class)
      .getClass());
    assertEquals(ItemUnifiedTableHeaderBuilder.class, modClientAdapterFactory.getModClientAdapter(Item.class)
      .getClass());
  }
}
