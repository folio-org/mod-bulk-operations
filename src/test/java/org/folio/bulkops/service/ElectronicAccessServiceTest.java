package org.folio.bulkops.service;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.exception.EntityFormatException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class ElectronicAccessServiceTest extends BaseTest {
  @MockitoBean
  private ElectronicAccessReferenceService electronicAccessReferenceService;
  @MockitoSpyBean
  private FolioExecutionContext folioExecutionContext;

  @Autowired
  private ElectronicAccessService electronicAccessService;
  @Autowired
  private FolioModuleMetadata folioModuleMetadata;

  @ParameterizedTest
  @CsvSource(value = {",,,,", "id,uri,text,specification,note"}, delimiter = ',')
  void testElectronicAccessToString(String relationshipId, String uri, String linkText,
                                    String materialsSpecification, String publicNote) {
    when(electronicAccessReferenceService.getRelationshipNameById("id")).thenReturn("name");
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);
    var actual = electronicAccessService.electronicAccessToString(ElectronicAccess.builder()
            .uri(uri)
            .linkText(linkText)
            .materialsSpecification(materialsSpecification)
            .publicNote(publicNote)
            .relationshipId(relationshipId)
            .tenantId("tenant")
            .build());

    if (isNull(uri)) {
      assertEquals("-\u001f;-\u001f;-\u001f;-\u001f;-", actual);
    } else {
      assertEquals("name\u001f;uri\u001f;text\u001f;specification\u001f;note", actual);
    }
  }

  @ParameterizedTest
  @CsvSource(value = {",,,,", "id,uri,text,specification,note"}, delimiter = ',')
  void testElectronicAccessInstanceToString(String relationshipId, String uri, String linkText,
                                            String materialsSpecification, String publicNote) {
    when(electronicAccessReferenceService.getRelationshipNameById("id")).thenReturn("name");
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);
    var actual = electronicAccessService.electronicAccessInstanceToString(ElectronicAccess.builder()
            .uri(uri)
            .linkText(linkText)
            .materialsSpecification(materialsSpecification)
            .publicNote(publicNote)
            .relationshipId(relationshipId)
            .tenantId("tenant")
            .build());

    if (isEmpty(uri)) {
      assertEquals("-\u001f;-\u001f;-\u001f;-\u001f;-", actual);
    } else {
      assertEquals("name\u001f;uri\u001f;text\u001f;specification\u001f;note", actual);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "name\u001f;uri\u001f;text\u001f;specification\u001f;note"})
  void testRestoreElectronicAccessItem(String s) {
    when(electronicAccessReferenceService.getRelationshipIdByName("name")).thenReturn("id");

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
    assertThrows(EntityFormatException.class,
            () -> electronicAccessService.restoreElectronicAccessItem(";;;"));
  }
}
