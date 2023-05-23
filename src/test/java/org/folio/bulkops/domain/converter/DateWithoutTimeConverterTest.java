package org.folio.bulkops.domain.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateWithoutTimeConverterTest {

  @Test
  void checkConvert() {
    try {
      var converter = new DateWithoutTimeConverter();
      converter.convert("2023-02-04");
    } catch (Exception ex) {
      Assertions.fail("Error converting date", ex);
    }
  }

  @Test
  void checkConvertToWrite() {
    var converter = new DateWithoutTimeConverter();
    var date = new Date(1684713600000L);
    var result = converter.convertToWrite(date);
    assertEquals("2023-05-22", result);
  }
}
