package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.Subject;
import org.folio.bulkops.exception.EntityFormatException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class SubjectServiceTest extends BaseTest {

  @Autowired private SubjectService subjectService;

  @MockitoBean private SubjectReferenceService subjectReferenceService;

  @Test
  void subjectToStringTest() {
    var sourceId = UUID.randomUUID().toString();
    var typeId = UUID.randomUUID().toString();

    when(subjectReferenceService.getSubjectSourceNameById(sourceId, null)).thenReturn("source");
    when(subjectReferenceService.getSubjectTypeNameById(typeId, null)).thenReturn("type");

    var subject = Subject.builder().value("heading").sourceId(sourceId).typeId(typeId).build();
    var actual = subjectService.subjectToString(subject);
    var expected = "heading\u001f;source\u001f;type";

    assertEquals(expected, actual);
  }

  @Test
  void restoreSubjectItemTest() {
    var sourceId = UUID.randomUUID().toString();
    var typeId = UUID.randomUUID().toString();

    when(subjectReferenceService.getSubjectSourceIdByName("source")).thenReturn(sourceId);
    when(subjectReferenceService.getSubjectTypeIdByName("type")).thenReturn(typeId);

    var subject = "heading\u001f;source\u001f;type";
    var actual = subjectService.restoreSubjectItem(subject);
    var expected = Subject.builder().value("heading").sourceId(sourceId).typeId(typeId).build();

    assertEquals(expected, actual);
  }

  @Test
  void restoreSubjectItemWrongNumberOfTokensTest() {
    var sourceId = UUID.randomUUID().toString();
    var typeId = UUID.randomUUID().toString();

    when(subjectReferenceService.getSubjectSourceIdByName("source")).thenReturn(sourceId);
    when(subjectReferenceService.getSubjectTypeIdByName("type")).thenReturn(typeId);

    var subject = "heading\u001f;source\u001f;type\u001f;";
    assertThrows(EntityFormatException.class, () -> subjectService.restoreSubjectItem(subject));
  }
}
