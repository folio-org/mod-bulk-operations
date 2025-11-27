package org.folio.bulkops.service;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.Publication;
import org.folio.bulkops.exception.EntityFormatException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PublicationServiceTest extends BaseTest {

  @Autowired private PublicationService publicationService;
  @MockitoBean private PublicationHelperService publicationHelperService;

  @Test
  void publicationToStringTest() {
    var publication =
        Publication.builder()
            .publisher("Springer")
            .role("Editor")
            .place("Berlin")
            .dateOfPublication("2020")
            .build();

    var res = publicationService.publicationToString(publication);

    Assertions.assertEquals("Springer\u001f;Editor\u001f;Berlin\u001f;2020", res);
  }

  @Test
  void publicationToString_shouldUseHyphenForMissingFields() {
    Publication publication =
        Publication.builder()
            .publisher(null)
            .role("")
            .place("Paris")
            .dateOfPublication(null)
            .build();

    var res = publicationService.publicationToString(publication);

    Assertions.assertEquals("-\u001f;-\u001f;Paris\u001f;-", res);
  }

  @Test
  void restorePublicationItem_shouldDeserializeCorrectly() {
    String input = String.join("\u001f;", "O'Reilly", "Author", "NY", "2023");

    Publication result = publicationService.restorePublicationItem(input);

    Assertions.assertEquals("O'Reilly", result.getPublisher());
    Assertions.assertEquals("Author", result.getRole());
    Assertions.assertEquals("NY", result.getPlace());
    Assertions.assertEquals("2023", result.getDateOfPublication());
  }

  @Test
  void restorePublicationItem_shouldThrowExceptionIfInvalidComponentCount() {
    String input = String.join("\u001f;", "Only", "Three", "Fields");

    Assertions.assertThrows(
        EntityFormatException.class, () -> publicationService.restorePublicationItem(input));
  }

  @Test
  void restorePublicationItem_shouldReturnNullForEmptyInput() {
    Assertions.assertNull(publicationService.restorePublicationItem(""));
    Assertions.assertNull(publicationService.restorePublicationItem(null));
  }
}
