package org.folio.bulkops.adapters.impl;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.adapters.Constants.ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.folio.bulkops.adapters.ElectronicAccessStringMapper;
import org.folio.bulkops.client.ElectronicAccessRelationshipClient;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.domain.bean.ElectronicAccessRelationship;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.service.ErrorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElectronicAccessStringMapperTest {

  @Mock
  private ElectronicAccessRelationshipClient relationshipClient;

  @Mock
  private ErrorService errorService;

  @InjectMocks
  private ElectronicAccessStringMapper electronicAccessStringMapper;

  @Test
  void getElectronicAccessesToStringTest() {
    var relationshipId = "relationshipId";
    var electronicAccess = new ElectronicAccess();
    electronicAccess.setRelationshipId(relationshipId);
    electronicAccess.setUri("uri");

    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(relationshipId);
    electronicAccessRelationship.setName("name");

    when(relationshipClient.getById(relationshipId)).thenReturn(electronicAccessRelationship);

    var expected = "uri;;;;name;relationshipId";
    var actual = electronicAccessStringMapper.getElectronicAccessesToString(List.of(electronicAccess), null, null);

    assertEquals(expected, actual);
  }

  @Test
  void getElectronicAccessesToStringElectronicAccessRelationshipNotFoundByIdTest() {
    var relationshipId1 = "relationshipId1";
    var electronicAccess1 = new ElectronicAccess();
    electronicAccess1.setRelationshipId(relationshipId1);
    electronicAccess1.setUri("uri1");

    var relationshipId2 = "relationshipId2";
    var electronicAccess2 = new ElectronicAccess();
    electronicAccess2.setRelationshipId(relationshipId2);
    electronicAccess2.setUri("uri2");

    var electronicAccess3 = new ElectronicAccess();
    electronicAccess3.setRelationshipId(relationshipId1);
    electronicAccess3.setUri("uri3");

    when(relationshipClient.getById(relationshipId1)).thenThrow(new NotFoundException("error message"));
    when(relationshipClient.getById(relationshipId2)).thenThrow(new NotFoundException("error message"));
    var expected = "uri1;;;;;relationshipId1|uri2;;;;;relationshipId2|uri3;;;;;relationshipId1";
    var actual = electronicAccessStringMapper.getElectronicAccessesToString(List.of(electronicAccess1, electronicAccess2, electronicAccess3), UUID.randomUUID(), "identifier");

    verify(errorService, times(2)).saveError(isA(UUID.class), isA(String.class), isA(String.class));
    assertEquals(expected, actual);
  }

  @Test
  void getElectronicAccessesToStringElectronicAccessRelationshipIsNullTest() {
    var electronicAccess = new ElectronicAccess();
    electronicAccess.setUri("uri");

    var expected = "uri;;;;;";
    var actual = electronicAccessStringMapper.getElectronicAccessesToString(List.of(electronicAccess), null, null);

    assertEquals(expected, actual);
  }

  @Test
  void getRelationshipNameAndIdByIdTest() {
    var id = UUID.randomUUID().toString();
    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(id);
    electronicAccessRelationship.setName("name");

    when(relationshipClient.getById(id)).thenReturn(electronicAccessRelationship);

    var expected = electronicAccessRelationship.getName() + ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER + electronicAccessRelationship.getId();
    var actual = electronicAccessStringMapper.getRelationshipNameAndIdById(id);

    verify(relationshipClient).getById(id);
    assertEquals(expected, actual);
  }

  @Test
  void getRelationshipNameAndIdByIdNotFoundExceptionTest() {
    var id = UUID.randomUUID().toString();
    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(id);
    electronicAccessRelationship.setName("name");

    when(relationshipClient.getById(id)).thenThrow(new NotFoundException("error message"));

    var expected = EMPTY +  ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER + electronicAccessRelationship.getId();
    var actual = electronicAccessStringMapper.getRelationshipNameAndIdById(id);

    assertEquals(expected, actual);
  }

}
