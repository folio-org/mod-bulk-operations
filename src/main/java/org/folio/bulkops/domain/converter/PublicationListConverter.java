package org.folio.bulkops.domain.converter;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.util.Constants.PUBLICATION_HEADINGS;
import static org.folio.bulkops.util.Constants.SPECIAL_ITEM_DELIMITER;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.bulkops.domain.bean.Publication;
import org.folio.bulkops.service.PublicationHelperService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PublicationListConverter extends BaseConverter<List<Publication>> {
  @Override
  public String convertToString(List<Publication> publications) {

    return ObjectUtils.isEmpty(publications) ?
      EMPTY :
      PUBLICATION_HEADINGS +
        publications.stream()
          .filter(Objects::nonNull)
          .map(PublicationHelperService.service()::publicationToString)
          .collect(Collectors.joining(SPECIAL_ITEM_DELIMITER));
  }
}

