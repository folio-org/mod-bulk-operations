package org.folio.bulkops.util;

import lombok.experimental.UtilityClass;
import org.folio.bulkops.domain.dto.IdentifierType;

import java.util.EnumMap;

@UtilityClass
public class BulkEditProcessorHelper {

  private static final EnumMap<IdentifierType, String> identifiersMap = new EnumMap<>(IdentifierType.class);

  public static String resolveIdentifier(String identifier) {
    return identifiersMap.get(IdentifierType.fromValue(identifier));
  }
}
