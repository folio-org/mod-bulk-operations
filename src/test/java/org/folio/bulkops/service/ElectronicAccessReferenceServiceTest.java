package org.folio.bulkops.service;

import static org.folio.bulkops.util.Constants.QUERY_PATTERN_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.folio.bulkops.client.ElectronicAccessRelationshipClient;
import org.folio.bulkops.domain.bean.ElectronicAccessRelationship;
import org.folio.bulkops.domain.bean.ElectronicAccessRelationshipCollection;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ElectronicAccessReferenceServiceTest {
  @Mock
  private ElectronicAccessRelationshipClient electronicAccessRelationshipClient;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @InjectMocks
  private ElectronicAccessReferenceService electronicAccessReferenceService;

  @Test
  void shouldReturnRelationshipNameById() {
    var id = "id;diku";
    var expectedName = "name";
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(electronicAccessRelationshipClient.getById(id)).thenReturn(new ElectronicAccessRelationship().withName(expectedName));

    var actualName = electronicAccessReferenceService.getRelationshipNameById(id, "diku");

    assertEquals(expectedName, actualName);
  }

  @Test
  void shouldReturnIdIfRelationshipNotFound() {
    var id = "id;tenant";
    var expectedName = id;

    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(electronicAccessRelationshipClient.getById(id)).thenThrow(new NotFoundException("Not found"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);

    var actualName = electronicAccessReferenceService.getRelationshipNameById(id, "tenant");

    assertEquals(expectedName, actualName);
  }

  @Test
  void shouldReturnRelationshipIdByName() {
    var name = "name";
    var expectedId = "id";
    when(electronicAccessRelationshipClient.getByQuery(String.format(QUERY_PATTERN_NAME, name)))
      .thenReturn(new ElectronicAccessRelationshipCollection()
        .withElectronicAccessRelationships(Collections.singletonList(new ElectronicAccessRelationship().withId(expectedId))));

    var actualId = electronicAccessReferenceService.getRelationshipIdByName(name);

    assertEquals(expectedId, actualId);
  }

  @Test
  void shouldReturnNameIfRelationshipNotFound() {
    var name = "name";
    var expectedId = name;
    when(electronicAccessRelationshipClient.getByQuery(String.format(QUERY_PATTERN_NAME, name)))
      .thenReturn(new ElectronicAccessRelationshipCollection().withElectronicAccessRelationships(Collections.emptyList()));

    var actualId = electronicAccessReferenceService.getRelationshipIdByName(name);

    assertEquals(expectedId, actualId);
  }
}
