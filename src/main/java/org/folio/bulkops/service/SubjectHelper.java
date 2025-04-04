package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.bean.Subject;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class SubjectHelper implements InitializingBean {

  private final SubjectService subjectService;

  public Subject restoreSubjectItem(@NotNull String subject) {
    return subjectService.restoreSubjectItem(subject);
  }

  public String subjectToString(Subject subject) {
    return subjectService.subjectToString(subject);
  }

  private static SubjectHelper service;

  @Override
  public void afterPropertiesSet() throws Exception {
    service = this;
  }

  public static SubjectHelper service() {
    return service;
  }

}
