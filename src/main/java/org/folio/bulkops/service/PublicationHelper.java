package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.Publication;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.InitializingBean;
import org.jetbrains.annotations.NotNull;

@Service
@RequiredArgsConstructor
@Log4j2
public class PublicationHelper implements InitializingBean {

  private final PublicationService publicationService;

  public Publication restorePublicationItem(@NotNull String publicationString) {
    return publicationService.restorePublicationItem(publicationString);
  }

  public String publicationToString(Publication publication) {
    return publicationService.publicationToString(publication);
  }

  private static PublicationHelper service;

  @Override
  public void afterPropertiesSet() throws Exception {
    service = this;
  }

  public static PublicationHelper service() {
    return service;
  }
}
