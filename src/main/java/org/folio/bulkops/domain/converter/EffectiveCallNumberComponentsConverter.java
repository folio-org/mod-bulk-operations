package org.folio.bulkops.domain.converter;

import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.bulkops.util.Utils.ofEmptyString;

import java.util.ArrayList;
import java.util.List;

import org.folio.bulkops.domain.bean.EffectiveCallNumberComponents;
import org.folio.bulkops.domain.format.SpecialCharacterEscaper;

public class EffectiveCallNumberComponentsConverter extends BaseConverter<EffectiveCallNumberComponents> {

  @Override
  public String convertToString(EffectiveCallNumberComponents object) {
    List<String> comps = new ArrayList<>();
    ofEmptyString(object.getPrefix()).map(SpecialCharacterEscaper::escape).ifPresent(comps::add);
    ofEmptyString(object.getCallNumber()).map(SpecialCharacterEscaper::escape).ifPresent(comps::add);
    ofEmptyString(object.getSuffix()).map(SpecialCharacterEscaper::escape).ifPresent(comps::add);
    return join(SPACE, comps);
  }
}
