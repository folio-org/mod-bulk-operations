package org.folio.bulkops.adapters;

import lombok.RequiredArgsConstructor;
import org.folio.bulkops.adapters.impl.holdings.HoldingModClientAdapter;
import org.folio.bulkops.adapters.impl.items.ItemModClientAdapter;
import org.folio.bulkops.adapters.impl.users.UserModClientAdapter;
import org.folio.bulkops.domain.dto.EntityType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModClientAdapterFactory {

  private final UserModClientAdapter userModClientAdapter;
  private final ItemModClientAdapter itemModClientAdapter;
  private final HoldingModClientAdapter holdingModClientAdapter;

  public ModClient<?> getModClientAdapter(EntityType type) {
    if (type == EntityType.ITEM) return itemModClientAdapter;
    if (type == EntityType.HOLDING) return holdingModClientAdapter;
    return userModClientAdapter;
  }
}
