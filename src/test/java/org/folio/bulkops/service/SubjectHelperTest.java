package org.folio.bulkops.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.entity.BulkOperationExecution;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class SubjectHelperTest extends BaseTest {

  @Autowired private SubjectHelper subjectHelper;

  @MockitoBean private SubjectService subjectService;

  @Captor private ArgumentCaptor<BulkOperationExecution> executionArgumentCaptor;

  @Test
  void restoreSubjectItemTest() {
    var subject = "heading\u001f;source\u001f;type";

    subjectHelper.restoreSubjectItem(subject);

    verify(subjectService, times(1)).restoreSubjectItem(subject);
  }
}
