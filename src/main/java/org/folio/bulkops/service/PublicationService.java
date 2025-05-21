package org.folio.bulkops.service;

import org.folio.bulkops.domain.bean.Publication;
import org.folio.bulkops.exception.EntityFormatException;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.folio.bulkops.util.Constants.HYPHEN;
import static org.folio.bulkops.util.Constants.SPECIAL_ARRAY_DELIMITER;

public class PublicationService {
  private static final int NUMBER_OF_PUBLICATION_COMPONENTS = 4;
  private static final int PUBLISHER_INDEX = 0;
  private static final int ROLE_INDEX = 1;
  private static final int PLACE_INDEX = 2;
  private static final int DATE_INDEX = 3;

  private String delimiter = SPECIAL_ARRAY_DELIMITER;

  public String publicationToString(Publication publication) {
    return String.join("|",
      defaultIfEmpty(publication.getPublisher(), HYPHEN),
      defaultIfEmpty(publication.getRole(), HYPHEN),
      defaultIfEmpty(publication.getPlace(), HYPHEN),
      defaultIfEmpty(publication.getDateOfPublication(), HYPHEN)
    );
  }

  public Publication restorePublicationItem(String publicationString) {
    if (isNotEmpty(publicationString)) {
      var tokens = publicationString.split(delimiter, -1);
      if (NUMBER_OF_PUBLICATION_COMPONENTS == tokens.length) {
        return Publication.builder()
          .publisher(tokens[PUBLISHER_INDEX])
          .role(tokens[ROLE_INDEX])
          .place(tokens[PLACE_INDEX])
          .dateOfPublication(tokens[DATE_INDEX])
          .build();
      }
      throw new EntityFormatException(String.format("Illegal number of publication elements: %d, expected: %d", tokens.length, NUMBER_OF_PUBLICATION_COMPONENTS));
    }
    return null;
  }
}
