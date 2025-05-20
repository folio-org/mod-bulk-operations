package org.folio.bulkops.domain.converter;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.Publication;
import org.folio.bulkops.service.PublicationHelper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.PUBLICATION;
import static org.folio.bulkops.util.Constants.SPECIAL_ITEM_DELIMITER;

public class PublicationListConverter extends BaseConverter<List<Publication>> {
  @Override
  public String convertToString(List<Publication> object) {
    return ObjectUtils.isEmpty(object) ?
      EMPTY :
      PUBLICATION +
        object.stream()
          .filter(Objects::nonNull)
          .map(PublicationHelper.service()::publicationToString)
          .collect(Collectors.joining(SPECIAL_ITEM_DELIMITER));
  }
}

