package org.folio.bulkops.service;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.bulkops.util.Constants.HYPHEN;
import static org.folio.bulkops.util.Constants.SPECIAL_ARRAY_DELIMITER;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.Publication;
import org.folio.bulkops.exception.EntityFormatException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class PublicationService {
  private static final int NUMBER_OF_PUBLICATION_COMPONENTS = 4;
  private static final int PUBLISHER_INDEX = 0;
  private static final int ROLE_INDEX = 1;
  private static final int PLACE_INDEX = 2;
  private static final int DATE_INDEX = 3;
  private static final String DELIMITER = SPECIAL_ARRAY_DELIMITER;

  public String publicationToString(Publication publication) {
    return String.join(DELIMITER,
      isEmpty(publication.getPublisher()) ? HYPHEN : publication.getPublisher(),
      isEmpty(publication.getRole()) ? HYPHEN : publication.getRole(),
      isEmpty(publication.getPlace()) ? HYPHEN : publication.getPlace(),
      isEmpty(publication.getDateOfPublication()) ? HYPHEN : publication.getDateOfPublication()
    );
  }


  public Publication restorePublicationItem(String publicationString) {
    if (isNotEmpty(publicationString)) {
      var tokens = publicationString.split(DELIMITER, -1);
      if (NUMBER_OF_PUBLICATION_COMPONENTS == tokens.length) {
        return Publication.builder()
          .publisher(tokens[PUBLISHER_INDEX])
          .role(tokens[ROLE_INDEX])
          .place(tokens[PLACE_INDEX])
          .dateOfPublication(tokens[DATE_INDEX])
          .build();
      }
      throw new EntityFormatException(
              String.format("Illegal number of publication elements: %d, expected: %d",
              tokens.length, NUMBER_OF_PUBLICATION_COMPONENTS));
    }
    return null;
  }
}

