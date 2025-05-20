package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.Publication;
import org.folio.bulkops.exception.EntityFormatException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.bulkops.util.Constants.*;
import static org.folio.bulkops.util.Constants.HYPHEN;

@Service
@RequiredArgsConstructor
@Log4j2
public class PublicationService {
  private static final int NUMBER_OF_PUBLICATION_COMPONENTS = 4;
  private static final int PUBLISHER_INDEX = 0;
  private static final int PUBLISHER_ROLE_INDEX = 1;
  private static final int PLACE_OF_PUBLICATION_INDEX = 2;
  private static final int PUBLICATION_DATE_INDEX = 3;

  private final PublicationReferenceService publicationReferenceService;

  private String delimiter = SPECIAL_ARRAY_DELIMITER;

  public String publicationToString(Publication publication) {
    return String.join(delimiter,
      isEmpty(publication.getPublisher()) ? HYPHEN : publication.getPublisher(),
      isEmpty(publication.getRole()) ? HYPHEN : publication.getRole(),
      isEmpty(publication.getPlace()) ? HYPHEN : publication.getPlace(),
      isEmpty(publication.getDateOfPublication()) ? HYPHEN : publication.getDateOfPublication()
    );
  }

  public Publication restorePublicationItem(@NotNull String publicationString) {
    if (isNotEmpty(publicationString)) {
      var tokens = publicationString.split(delimiter, -1);
      if (NUMBER_OF_PUBLICATION_COMPONENTS == tokens.length) {
        return Publication.builder()
          .publisher(HYPHEN.equals(tokens[PUBLISHER_INDEX]) ? null : tokens[PUBLISHER_INDEX])
          .role(HYPHEN.equals(tokens[PUBLISHER_ROLE_INDEX]) ? null : tokens[PUBLISHER_ROLE_INDEX])
          .place(HYPHEN.equals(tokens[PLACE_OF_PUBLICATION_INDEX]) ? null : tokens[PLACE_OF_PUBLICATION_INDEX])
          .dateOfPublication(HYPHEN.equals(tokens[PUBLICATION_DATE_INDEX]) ? null : tokens[PUBLICATION_DATE_INDEX])
          .build();
      }
      throw new EntityFormatException(String.format(
        "Illegal number of publication elements: %d, expected: %d",
        tokens.length, NUMBER_OF_PUBLICATION_COMPONENTS));
    }
    return null;
  }
}

