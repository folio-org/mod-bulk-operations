package org.folio.bulkops.util;

import feign.FeignException;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.bean.BulkOperationsEntity;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.User;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.MatchResult;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.MSG_ERROR_OPTIMISTIC_LOCKING_DEFAULT;
import static org.folio.bulkops.util.Constants.MSG_ERROR_TEMPLATE_OPTIMISTIC_LOCKING;

@UtilityClass
@Log4j2
public class Utils {

  private static final Pattern PATTERN = Pattern.compile("\\s\\d+\\s|\\s\\d+,");
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
      case INSTANCE -> Instance.class;
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

  public static String getMessageFromFeignException(FeignException e) {
    try {
      String[] matches = PATTERN
        .matcher(e.getMessage())
        .results()
        .map(MatchResult::group)
        .map(String::trim)
        .toArray(String[]::new);
      if (matches.length == 2) {
        return format(MSG_ERROR_TEMPLATE_OPTIMISTIC_LOCKING, matches[0], matches[1]);
      } else {
        log.warn("Error extracting entity versions from: {}. Returning default message", e.getMessage());
        return MSG_ERROR_OPTIMISTIC_LOCKING_DEFAULT;
      }
    } catch (Exception ex) {
      log.warn("Error parsing message with entity versions: {}. Returning default message", e.getMessage(), ex);
      return MSG_ERROR_OPTIMISTIC_LOCKING_DEFAULT;
    }
  }
}
