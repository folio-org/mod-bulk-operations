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
  public String publicationToString(Publication pub) {
    if (pub == null) {
      return ";;;";
    }

    return String.join(";",
      nullSafe(pub.getPublisher()),              // Publisher
      nullSafe(pub.getRole()),                   // Publisher role
      nullSafe(pub.getPlace()),                  // Place of publication
      nullSafe(pub.getDateOfPublication())       // Publication date
    );
  }

  private static String nullSafe(String val) {
    return val == null ? "" : val.trim();
  }

  private static PublicationHelperService service;

  @Override
  public void afterPropertiesSet() {
    service = this;
  }

  public static PublicationHelperService service() {
    return service;
  }
}

