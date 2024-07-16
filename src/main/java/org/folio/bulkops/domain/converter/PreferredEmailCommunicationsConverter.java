package org.folio.bulkops.domain.converter;

import org.folio.bulkops.domain.bean.PreferredEmailCommunications;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;

public class PreferredEmailCommunicationsConverter extends BaseConverter<Set<PreferredEmailCommunications>> {

  @Override
  public Set<PreferredEmailCommunications> convertToObject(String value) {
    if (isNotEmpty(value)) {
      return Arrays.stream(value.split(ARRAY_DELIMITER)).map(PreferredEmailCommunications::fromValue).collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  @Override
  public String convertToString(Set<PreferredEmailCommunications> object) {
    return ofNullable(String.join(ARRAY_DELIMITER, object.stream().map(PreferredEmailCommunications::getValue).toList())).orElse(EMPTY);
  }
}
