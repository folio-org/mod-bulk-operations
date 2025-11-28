package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.folio.bulkops.domain.bean.PreferredEmailCommunication;

public class PreferredEmailCommunicationConverter
    extends BaseConverter<Set<PreferredEmailCommunication>> {

  @Override
  public Set<PreferredEmailCommunication> convertToObject(String value) {
    if (isNotEmpty(value)) {
      return Arrays.stream(value.split(ARRAY_DELIMITER))
          .map(PreferredEmailCommunication::fromValue)
          .collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  @Override
  public String convertToString(Set<PreferredEmailCommunication> object) {
    return Optional.of(
            String.join(
                ARRAY_DELIMITER,
                object.stream().map(PreferredEmailCommunication::getValue).toList()))
        .orElse(EMPTY);
  }
}
