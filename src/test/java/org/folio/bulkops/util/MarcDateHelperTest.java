package org.folio.bulkops.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.marc4j.marc.impl.ControlFieldImpl;
import org.marc4j.marc.impl.RecordImpl;

import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarcDateHelperTest {

  @Test
  @SneakyThrows
  void getDateTimeForMarcTest() {
    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 10:02:02.454");

    var actual = MarcDateHelper.getDateTimeForMarc(date);
    assertEquals("20240101100202.4", actual);
  }

  @Test
  @SneakyThrows
  void shouldUpdateDateTimeControlField() {
    var pattern = "yyyy/MM/dd HH:mm:ss.SSS";
    var simpleDateFormat = new SimpleDateFormat(pattern);
    var date = simpleDateFormat.parse("2024/01/01 10:02:02.454");

    var marcRecord = new RecordImpl();
    var dateTimeControlField = new ControlFieldImpl("005", "20240101090202.1");
    marcRecord.addVariableField(dateTimeControlField);

    MarcDateHelper.updateDateTimeControlField(marcRecord, date);

    assertEquals("20240101100202.4", marcRecord.getControlFields().get(0).getData());
  }
}
