package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.Classification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.UUID;

class ClassificationServiceTest extends BaseTest {

  @Autowired
  private ClassificationService classificationService;

  @MockitoBean
  private ClassificationReferenceService classificationReferenceService;

  @Test
  void classificationToStringTest() {
    var classificationTypeId = UUID.randomUUID().toString();
    var classification = Classification.builder()
      .classificationTypeId(classificationTypeId)
      .classificationNumber("abc")
      .build();

    when(classificationReferenceService.getClassificationTypeNameById(classificationTypeId, null))
      .thenReturn("LC");

    var res = classificationService.classificationToString(classification);

    assertThat(res).isEqualTo("LC\u001F;abc");
  }

  @Test
  void restoreClassificationItemTest() {
    var classificationTypeId = UUID.randomUUID().toString();
    var classificationString = "LC\u001F;abc";

    when(classificationReferenceService.getClassificationTypeIdByName("LC"))
      .thenReturn(classificationTypeId);

    var res = classificationService.restoreClassificationItem(classificationString);

    assertThat(res.getClassificationTypeId()).isEqualTo(classificationTypeId);
    assertThat(res.getClassificationNumber()).isEqualTo("abc");
  }
}
