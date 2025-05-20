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
        var publisher = tokens[PUBLISHER_INDEX];
        var role = tokens[PUBLISHER_ROLE_INDEX];
        var place = tokens[PLACE_OF_PUBLICATION_INDEX];
        var date = tokens[PUBLICATION_DATE_INDEX];

        Publication refPublication = HYPHEN.equals(publisher)
          ? null
          : publicationReferenceService.getPublicationByPublisherName(publisher);

        return Publication.builder()
          .publisher(refPublication != null ? refPublication.getPublisher() : (HYPHEN.equals(publisher) ? null : publisher))
          .role(refPublication != null ? refPublication.getRole() : (HYPHEN.equals(role) ? null : role))
          .place(refPublication != null ? refPublication.getPlace() : (HYPHEN.equals(place) ? null : place))
          .dateOfPublication(refPublication != null ? refPublication.getDateOfPublication() : (HYPHEN.equals(date) ? null : date))
          .build();
      }

      throw new EntityFormatException(String.format(
        "Illegal number of publication elements: %d, expected: %d",
        tokens.length, NUMBER_OF_PUBLICATION_COMPONENTS));
    }
    return null;
  }




}

