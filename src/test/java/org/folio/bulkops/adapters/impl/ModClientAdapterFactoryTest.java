package org.folio.bulkops.adapters.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.adapters.ModClientAdapterFactory;
import org.folio.bulkops.adapters.impl.holdings.HoldingModClientAdapter;
import org.folio.bulkops.adapters.impl.items.ItemModClientAdapter;
import org.folio.bulkops.adapters.impl.users.UserModClientAdapter;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ModClientAdapterFactoryTest extends BaseTest {
  @Autowired
  private ModClientAdapterFactory modClientAdapterFactory;
  @Test
  void getModClientAdapterTest() {
    assertEquals(UserModClientAdapter.class, modClientAdapterFactory.getModClientAdapter(User.class)
      .getClass());
    assertEquals(HoldingModClientAdapter.class, modClientAdapterFactory.getModClientAdapter(HoldingsRecord.class)
      .getClass());
    assertEquals(ItemModClientAdapter.class, modClientAdapterFactory.getModClientAdapter(Item.class)
      .getClass());

  }
}
