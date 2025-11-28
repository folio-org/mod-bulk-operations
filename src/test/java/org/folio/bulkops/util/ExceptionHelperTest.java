package org.folio.bulkops.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;

class ExceptionHelperTest {

  @Test
  void fetchMessage_returnsRootCauseMessageForNormalException() {
    Exception ex =
        new RuntimeException(
            "Top", new IllegalArgumentException("Root cause\n" + "with line break"));
    String message = ExceptionHelper.fetchMessage(ex);
    assertThat(message).isEqualTo("IllegalArgumentException: Root cause with line break");
  }

  @Test
  void fetchMessage_returnsFormattedMessageForInvalidFormatException() {
    JsonMappingException.Reference ref1 =
        new JsonMappingException.Reference(Object.class, "field1");
    JsonMappingException.Reference ref2 =
        new JsonMappingException.Reference(Object.class, "field2");

    InvalidFormatException ife =
        InvalidFormatException.from(null, "bad", "badValue", Integer.class);
    ife.prependPath(ref1);
    ife.prependPath(ref2);

    String message = ExceptionHelper.fetchMessage(ife);

    // The path is field2.field1 due to prepend order
    String expected = "Failed to parse Integer from value \"badValue\" in field2.field1";
    assertThat(message).isEqualTo(expected);
  }
}
