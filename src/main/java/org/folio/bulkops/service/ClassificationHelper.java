package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.Classification;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ClassificationHelper implements InitializingBean {

  private final ClassificationService classificationService;

  public Classification restoreClassificationItem(@NotNull String classificationString) {
    return classificationService.restoreClassificationItem(classificationString);
  }

  public String classificationToString(Classification classification) {
    return classificationService.classificationToString(classification);
  }

  private static ClassificationHelper service;

  @Override
  public void afterPropertiesSet() throws Exception {
    service = this;
  }

  public static ClassificationHelper service() {
    return service;
  }
}
