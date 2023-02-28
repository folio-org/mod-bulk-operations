package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.ElectronicAccessRelationshipClient;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.domain.bean.ElectronicAccessRelationship;
import org.folio.bulkops.exception.EntityFormatException;
import org.folio.bulkops.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ElectronicAccessServiceTest extends BaseTest {

  @Autowired
  private ElectronicAccessService electronicAccessService;
  @ParameterizedTest
  @CsvSource(value = { ",,,,", "uri,text,specification,note,id" }, delimiter = ',')
  void testElectronicAccessToString(String uri, String linkText, String materialsSpecification, String publicNote, String relationShipId) {
    when(relationshipClient.getById("id")).thenReturn(new ElectronicAccessRelationship().withName("name"));
    var actual = electronicAccessService.electronicAccessToString(ElectronicAccess.builder()
        .uri(uri)
        .linkText(linkText)
        .materialsSpecification(materialsSpecification)
        .publicNote(publicNote)
        .relationshipId(relationShipId)
      .build());

    if (isNull(uri)) {
      assertEquals("null;;;;;", actual);
    } else {
      assertEquals("uri;text;specification;note;name;id", actual);
    }

    when(relationshipClient.getById("id")).thenThrow(new NotFoundException("Not found"));
    actual = electronicAccessService.electronicAccessToString(ElectronicAccess.builder()
      .uri(uri)
      .linkText(linkText)
      .materialsSpecification(materialsSpecification)
      .publicNote(publicNote)
      .relationshipId(relationShipId)
      .build());

    if (isNull(uri)) {
      assertEquals("null;;;;;", actual);
    } else {
      assertEquals("uri;text;specification;note;;id", actual);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "uri;text;specification;note;name;id"})
  void testRestoreElectronicAccessItem(String s) {
    var item = electronicAccessService.restoreElectronicAccessItem(s);

    if (StringUtils.isEmpty(s)) {
      assertNull(item);
    } else {
      assertEquals(ElectronicAccess.builder()
        .uri("uri")
        .linkText("text")
        .materialsSpecification("specification")
        .publicNote("note")
        .relationshipId("id")
        .build(), item);
    }
  }

  @Test
  void testRestoreInvalidElectronicAccessItem() {
    assertThrows(EntityFormatException.class, () -> electronicAccessService.restoreElectronicAccessItem(";;;"));
  }
}
