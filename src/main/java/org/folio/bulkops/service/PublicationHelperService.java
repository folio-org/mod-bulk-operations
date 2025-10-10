package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.Publication;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Log4j2
public class PublicationHelperService implements InitializingBean {

  private final PublicationService publicationService;

  public Publication restorePublicationItem(String str) {
    return publicationService.restorePublicationItem(str);
  }

  public String publicationToString(Publication publication) {
    return publicationService.publicationToString(publication);
  }

  public static PublicationHelperService service;

  @Override
  public void afterPropertiesSet() throws Exception {
    service = this;
  }

  public static PublicationHelperService service() {
    return service;
  }

}
