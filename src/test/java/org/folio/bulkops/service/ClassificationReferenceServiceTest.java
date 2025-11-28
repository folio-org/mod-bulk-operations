package org.folio.bulkops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.folio.bulkops.client.ClassificationTypesClient;
import org.folio.bulkops.domain.bean.ClassificationType;
import org.folio.bulkops.domain.bean.ClassificationTypeCollection;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassificationReferenceServiceTest {

  @Mock private ClassificationTypesClient classificationTypesClient;
  @Mock private FolioExecutionContext folioExecutionContext;
  @InjectMocks private ClassificationReferenceService classificationReferenceService;

  @Test
  void shouldReturnClassificationTypeNameById() {

    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    var classificationTypeId = UUID.randomUUID().toString();
    when(classificationTypesClient.getById(classificationTypeId))
        .thenReturn(ClassificationType.builder().id(classificationTypeId).name("LC").build());

    var name =
        classificationReferenceService.getClassificationTypeNameById(classificationTypeId, null);

    assertThat(name).isEqualTo("LC");
  }

  @Test
  void shouldReturnClassificationTypeIdByName() {
    var classificationTypeId = UUID.randomUUID().toString();
    var name = "LC";

    when(classificationTypesClient.getByQuery(QUERY_PATTERN_NAME.formatted(name)))
        .thenReturn(
            ClassificationTypeCollection.builder()
                .classificationTypes(
                    Collections.singletonList(
                        ClassificationType.builder().id(classificationTypeId).name("LC").build()))
                .totalRecords(1)
                .build());

    var id = classificationReferenceService.getClassificationTypeIdByName(name);

    assertThat(id).isEqualTo(classificationTypeId);
  }
}
