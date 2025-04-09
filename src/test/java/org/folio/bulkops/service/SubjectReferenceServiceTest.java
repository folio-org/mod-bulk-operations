package org.folio.bulkops.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.folio.bulkops.client.SubjectSourcesClient;
import org.folio.bulkops.client.SubjectTypesClient;
import org.folio.bulkops.domain.bean.SubjectSource;
import org.folio.bulkops.domain.bean.SubjectType;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class SubjectReferenceServiceTest {

  @Mock
  private SubjectTypesClient subjectTypesClient;
  @Mock
  private SubjectSourcesClient subjectSourcesClient;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @InjectMocks
  private SubjectReferenceService subjectReferenceService;

  @Test
  void shouldReturnSubjectSourceNameById() {
    var id = "id";
    var expectedName = "name";
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(subjectSourcesClient.getById(id)).thenReturn(new SubjectSource().withName(expectedName));

    var actualName = subjectReferenceService.getSubjectSourceNameById(id, null);

    assertEquals(expectedName, actualName);
  }

  @Test
  void shouldReturnIdIfSubjectSourceNotFound() {
    var id = "id";
    var expectedName = id;

    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(subjectSourcesClient.getById(id)).thenThrow(new NotFoundException("Not found"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);

    var actualName = subjectReferenceService.getSubjectSourceNameById(id, null);

    assertEquals(expectedName, actualName);
  }

  @Test
  void shouldReturnSubjectTypeNameById() {
    var id = "id";
    var expectedName = "name";
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(subjectTypesClient.getById(id)).thenReturn(new SubjectType().withName(expectedName));

    var actualName = subjectReferenceService.getSubjectTypeNameById(id, null);

    assertEquals(expectedName, actualName);
  }

  @Test
  void shouldReturnIdIfSubjectTypeNotFound() {
    var id = "id";
    var expectedName = id;

    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(subjectTypesClient.getById(id)).thenThrow(new NotFoundException("Not found"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);

    var actualName = subjectReferenceService.getSubjectTypeNameById(id, null);

    assertEquals(expectedName, actualName);
  }

}
