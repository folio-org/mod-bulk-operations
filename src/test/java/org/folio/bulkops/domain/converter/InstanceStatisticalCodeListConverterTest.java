package org.folio.bulkops.domain.converter;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.domain.bean.StatisticalCodeCollection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class InstanceStatisticalCodeListConverterTest extends BaseTest {

  private final String id1 = UUID.randomUUID().toString();
  private final String id2 = UUID.randomUUID().toString();

  @Test
  void convertToObjectWhenTwoStatisticalCodesTest() {
    var converter = new InstanceStatisticalCodeListConverter();

    when(statisticalCodeClient.getByQuery("name==\"some code\""))
      .thenReturn(StatisticalCodeCollection.builder().statisticalCodes(
        List.of(StatisticalCode.builder().name("some code").id(id1).build())).build());

    when(statisticalCodeClient.getByQuery("name==\"some code 2\""))
      .thenReturn(StatisticalCodeCollection.builder().statisticalCodes(
        List.of(StatisticalCode.builder().name("some code 2").id(id2).build())).build());

    var actual = converter.convertToObject("some code;some code 2");
    assertEquals("[" + id1 + ", " + id2 + "]", actual.toString());
  }

  @Test
  void convertToStringWhenOneStatisticalCodesTest() {
    var converter = new InstanceStatisticalCodeListConverter();

    when(statisticalCodeClient.getById(id1)).thenReturn(StatisticalCode.builder().name("some code").id(id1).build());

    var actual = converter.convertToString(List.of(id1));
    assertEquals("some code", actual);
  }
}
