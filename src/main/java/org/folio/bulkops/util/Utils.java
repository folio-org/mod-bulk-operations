package org.folio.bulkops.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;

import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@UtilityClass
public class Utils {
  public static Optional<String> ofEmptyString(String string) {
    return StringUtils.isNotEmpty(string) ? Optional.of(string) : Optional.empty();
  }

  public static String encode(CharSequence s) {
    if (s == null) {
      return "\"\"";
    }
    var appendable = new StringBuilder(s.length() + 2);
    appendable.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\', '*', '?', '^', '"' -> appendable.append('\\').append(c);
        default -> appendable.append(c);
      }
    }
    appendable.append('"');
    return appendable.toString();
  }

  public static Class<? extends BulkOperationsEntity> resolveEntityClass(EntityType clazz) {
    return switch (clazz) {
      case USER -> User.class;
      case ITEM -> Item.class;
      case HOLDINGS_RECORD -> HoldingsRecord.class;
    };
  }

  public static String booleanToStringNullSafe(Boolean value) {
    return Objects.isNull(value) ? EMPTY : value.toString();
  }

  public static String getIdentifierForManualApproach(String[] line, IdentifierType identifierType) {
    return  switch (identifierType) {
      case BARCODE -> line[3];
      case EXTERNAL_SYSTEM_ID -> line[2];
      case USER_NAME -> line[0];
      default -> line[1];
    };
  }
}
