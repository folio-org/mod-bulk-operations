package org.folio.bulkops.util;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;

import java.util.Optional;

public class Utils {
  public static Optional<String> ofEmptyString(String string) {
    return StringUtils.isNotEmpty(string) ? Optional.of(string) : Optional.empty();
  }

  public static Class<? extends BulkOperationsEntity> resolveEntityClass(EntityType clazz) {
    return switch (clazz) {
      case USER -> User.class;
      case ITEM -> Item.class;
      case HOLDINGS_RECORD -> HoldingsRecord.class;
    };
  }
}
