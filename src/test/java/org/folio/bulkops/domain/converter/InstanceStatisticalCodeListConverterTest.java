package org.folio.bulkops.domain.converter;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.StatisticalCode;
import org.folio.bulkops.domain.bean.StatisticalCodeCollection;
import org.folio.bulkops.domain.bean.StatisticalCodeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class InstanceStatisticalCodeListConverterTest extends BaseTest {

  private final String statisticalCodeId1 = UUID.randomUUID().toString();
  private final String statisticalCodeId2 = UUID.randomUUID().toString();
  private final String statisticalCodeTypeId1 = UUID.randomUUID().toString();
  private final String statisticalCodeTypeId2 = UUID.randomUUID().toString();


  @Test
  void convertToObjectWhenTwoStatisticalCodesTest() {
    var converter = new InstanceStatisticalCodeListConverter();

    when(statisticalCodeClient.getByQuery("name==\"some name\""))
      .thenReturn(StatisticalCodeCollection.builder().statisticalCodes(
        List.of(StatisticalCode.builder().name("some code").statisticalCodeTypeId(statisticalCodeTypeId1).id(statisticalCodeId1).build())).build());

    when(statisticalCodeClient.getByQuery("name==\"some name 2\""))
      .thenReturn(StatisticalCodeCollection.builder().statisticalCodes(
        List.of(StatisticalCode.builder().name("some code 2").statisticalCodeTypeId(statisticalCodeTypeId2).id(statisticalCodeId2).build())).build());

    when(statisticalCodeTypeClient.getById(statisticalCodeTypeId1)).thenReturn(StatisticalCodeType.builder().id(statisticalCodeTypeId1).name("some type").build());

    when(statisticalCodeTypeClient.getById(statisticalCodeTypeId2)).thenReturn(StatisticalCodeType.builder().id(statisticalCodeTypeId2).name("some type 2").build());

    var actual = converter.convertToObject("some type: some code - some name|some type 2: some code 2 - some name 2");
    assertEquals("[" + statisticalCodeId1 + ", " + statisticalCodeId2 + "]", actual.toString());
  }

  @Test
  void convertToStringWhenOneStatisticalCodesTest() {
    var converter = new InstanceStatisticalCodeListConverter();

    when(statisticalCodeClient.getById(statisticalCodeId1)).thenReturn(StatisticalCode.builder().name("some name").statisticalCodeTypeId(statisticalCodeTypeId1).code("some code").id(statisticalCodeId1).build());
    when(statisticalCodeTypeClient.getById(statisticalCodeTypeId1)).thenReturn(StatisticalCodeType.builder().id(statisticalCodeTypeId1).name("some type").build());

    var actual = converter.convertToString(List.of(statisticalCodeId1));
    assertEquals("some type: some code - some name", actual);
  }
}
