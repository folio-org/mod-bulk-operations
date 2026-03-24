package org.folio.bulkops.domain.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BooleanConverterTest {

  private final BooleanConverter converter = new BooleanConverter();

  @Test
  void convertToString_whenObjectIsNull_returnsFalse() {
    String result = converter.convertToString(null);
    assertEquals("false", result);
  }

  @Test
  void convertToString_whenObjectIsTrue_returnsTrueString() {
    String result = converter.convertToString(Boolean.TRUE);
    assertEquals("true", result);
  }

  @Test
  void convertToString_whenObjectIsFalse_returnsFalseString() {
    String result = converter.convertToString(Boolean.FALSE);
    assertEquals("false", result);
  }
}

