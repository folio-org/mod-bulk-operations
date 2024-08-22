package org.folio.bulkops.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateHelperTest {

  @Test
  @SneakyThrows
  void getDateTimeForMarc() {
    String pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 10:02:02.454");

    var actual = DateHelper.getDateTimeForMarc(date);
    assertEquals("20240101100202.4", actual);
  }
}
