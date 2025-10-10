package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.QUERY_PATTERN_CODE;
import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.folio.bulkops.client.SubjectSourcesClient;
import org.folio.bulkops.client.SubjectTypesClient;
import org.folio.bulkops.domain.bean.SubjectSource;
import org.folio.bulkops.domain.bean.SubjectSourceCollection;
import org.folio.bulkops.domain.bean.SubjectType;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    var id = "id";
    var expectedName = "name";
    when(subjectSourcesClient.getById(id)).thenReturn(new SubjectSource().withName(expectedName));

    var actualName = subjectReferenceService.getSubjectSourceNameById(id, null);

    assertEquals(expectedName, actualName);
  }

  @Test
  void shouldReturnIdIfSubjectSourceNotFound() {

    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    var id = "id";
    when(subjectSourcesClient.getById(id)).thenThrow(new NotFoundException("Not found"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);

    var actualName = subjectReferenceService.getSubjectSourceNameById(id, null);

    assertEquals(id, actualName);
  }

  @Test
  void shouldReturnSubjectTypeNameById() {
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    var id = "id";
    var expectedName = "name";
    when(subjectTypesClient.getById(id)).thenReturn(new SubjectType().withName(expectedName));

    var actualName = subjectReferenceService.getSubjectTypeNameById(id, null);

    assertEquals(expectedName, actualName);
  }

  @Test
  void shouldReturnIdIfSubjectTypeNotFound() {
    var id = "id";

    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(subjectTypesClient.getById(id)).thenThrow(new NotFoundException("Not found"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);

    var actualName = subjectReferenceService.getSubjectTypeNameById(id, null);

    assertEquals(id, actualName);
  }

  @Test
  void shouldReturnSubjectSourceIdByName() {
    var name = "TestName";
    var expectedId = "123";
    var subjectSource = new SubjectSource().withId(expectedId);
    var subjectSources = new SubjectSourceCollection().withSubjectSources(List.of(subjectSource));

    when(subjectSourcesClient.getByQuery(String.format(QUERY_PATTERN_NAME, name)))
            .thenReturn(subjectSources);

    var actualId = subjectReferenceService.getSubjectSourceIdByName(name);

    assertEquals(expectedId, actualId);
  }

  @Test
  void shouldReturnNameIfSubjectSourceNotFoundByName() {
    var name = "UnknownName";
    var subjectSources = new SubjectSourceCollection().withSubjectSources(List.of());

    when(subjectSourcesClient.getByQuery(String.format(QUERY_PATTERN_NAME, name)))
            .thenReturn(subjectSources);

    var actualId = subjectReferenceService.getSubjectSourceIdByName(name);

    assertEquals(name, actualId);
  }

  @Test
  void shouldReturnSubjectTypeIdByName() {
    var name = "TestType";
    var expectedId = "type123";
    var subjectType = new SubjectType().withId(expectedId);
    var subjectTypes = new org.folio.bulkops.domain.bean.SubjectTypeCollection()
            .withSubjectTypes(List.of(subjectType));

    when(subjectTypesClient.getByQuery(String.format(QUERY_PATTERN_NAME, name)))
            .thenReturn(subjectTypes);

    var actualId = subjectReferenceService.getSubjectTypeIdByName(name);

    assertEquals(expectedId, actualId);
  }

  @Test
  void shouldReturnNameIfSubjectTypeNotFoundByName() {
    var name = "UnknownType";
    var subjectTypes = new org.folio.bulkops.domain.bean.SubjectTypeCollection()
            .withSubjectTypes(List.of());

    when(subjectTypesClient.getByQuery(String.format(QUERY_PATTERN_NAME, name)))
            .thenReturn(subjectTypes);

    var actualId = subjectReferenceService.getSubjectTypeIdByName(name);

    assertEquals(name, actualId);
  }

  @Test
  void shouldReturnHyphenIfSubjectSourceNotFoundByCode() {
    var code = "UnknownCode";
    var subjectSources = new SubjectSourceCollection().withSubjectSources(List.of());

    when(subjectSourcesClient.getByQuery(String.format(QUERY_PATTERN_CODE, code)))
            .thenReturn(subjectSources);

    var actualName = subjectReferenceService.getSubjectSourceNameByCode(code);

    assertEquals("-", actualName);
  }

  @Test
  void shouldReturnSubjectSourceNameByCode() {
    var code = "KnownCode";
    var expectedName = "SourceName";
    var subjectSource = new SubjectSource().withName(expectedName);
    var subjectSources = new SubjectSourceCollection().withSubjectSources(List.of(subjectSource));

    when(subjectSourcesClient.getByQuery(String.format(QUERY_PATTERN_CODE, code)))
            .thenReturn(subjectSources);

    var actualName = subjectReferenceService.getSubjectSourceNameByCode(code);

    assertEquals(expectedName, actualName);
  }

  @Test
  void shouldReturnHyphenIfSubjectSourceNameByCodeIsNull() {
    var code = "NullNameCode";
    var subjectSource = new SubjectSource().withName(null);
    var subjectSources = new SubjectSourceCollection().withSubjectSources(List.of(subjectSource));

    when(subjectSourcesClient.getByQuery(String.format(QUERY_PATTERN_CODE, code)))
            .thenReturn(subjectSources);

    var actualName = subjectReferenceService.getSubjectSourceNameByCode(code);

    assertEquals("-", actualName);
  }

}
