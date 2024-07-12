package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.PreferredEmailCommunication;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

public class PreferredEmailCommunicationConverter extends BaseConverter<Set<PreferredEmailCommunication>> {

  @Override
  public Set<PreferredEmailCommunication> convertToObject(String value) {
    if (isNotEmpty(value)) {
      return Arrays.stream(value.split(ARRAY_DELIMITER)).map(PreferredEmailCommunication::valueOf).collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  @Override
  public String convertToString(Set<PreferredEmailCommunication> object) {
    return ofNullable(String.join(ARRAY_DELIMITER, object.stream().map(PreferredEmailCommunication::getValue).toList())).orElse(EMPTY);
  }
}
