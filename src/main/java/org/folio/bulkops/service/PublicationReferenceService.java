package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.PublicationClient;
import org.folio.bulkops.domain.bean.Publication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;

@Service
@RequiredArgsConstructor
@Log4j2
public class PublicationReferenceService {

  private final PublicationClient publicationClient;

  @Cacheable(cacheNames = "publicationDetails")
  public Publication getPublicationByPublisherName(String name) {
    List<Publication> publications = publicationClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));

    if (publications == null || publications.isEmpty()) {
      return null;
    }

    return publications.stream()
      .filter(p -> name.equalsIgnoreCase(p.getPublisher()))
      .findFirst()
      .orElse(publications.get(0));
  }
}
