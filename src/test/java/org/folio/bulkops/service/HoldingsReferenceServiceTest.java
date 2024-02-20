package org.folio.bulkops.service;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.LocationClient;
import org.folio.bulkops.domain.bean.EffectiveCallNumberComponents;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class HoldingsReferenceServiceTest {

  @Mock
  private HoldingsClient holdingsClient;
  @Mock
  private LocationClient locationClient;
  @InjectMocks
  private HoldingsReferenceService holdingsReferenceService;

  @ParameterizedTest
  @MethodSource("emptyResultMethodSourceArgs")
  void emptyEffectiveLocationCallNumber(Item item) {
    var result = holdingsReferenceService.getEffectiveLocationCallNumberComponentsForItem(item);
    assertTrue(StringUtils.isBlank(result));
  }
  @Test
  void emptyEffectiveLocationCallNumberBecauseOfHoldingsRefData() {
    var holdingRecordId = UUID.randomUUID().toString();
    var effectiveLocationId = UUID.randomUUID().toString();
    var item = Item.builder()
      .holdingsRecordId(holdingRecordId)
      .permanentLocation(ItemLocation.builder().build())
      .build();

    when(holdingsClient.getHoldingById(holdingRecordId)).thenReturn(
      HoldingsRecord.builder()
        .effectiveLocationId(effectiveLocationId)
        .build()
    );

    when(locationClient.getLocationById(effectiveLocationId)).thenReturn(ItemLocation.builder().build());

    var result = holdingsReferenceService.getEffectiveLocationCallNumberComponentsForItem(item);
    assertTrue(StringUtils.isBlank(result));
  }

  @Test
  void notEmptyEffectiveLocationCallNumberBecauseOfItemContent() {
    var item = Item.builder()
      .holdingsRecordId(UUID.randomUUID().toString())
      .effectiveLocation(ItemLocation.builder().name("Main Library").build())
      .effectiveCallNumberComponents(EffectiveCallNumberComponents.builder().callNumber("TK5105.88815 . A58 2004 FT MEADE").build())
      .build();

    var result = holdingsReferenceService.getEffectiveLocationCallNumberComponentsForItem(item);
    assertEquals("Main Library > TK5105.88815 . A58 2004 FT MEADE", result);
  }

  @Test
  void notEmptyEffectiveLocationCallNumberBecauseOfHoldingsRefData() {
    var holdingRecordId = UUID.randomUUID().toString();
    var effectiveLocationId = UUID.randomUUID().toString();
    var item = Item.builder()
      .holdingsRecordId(holdingRecordId)
      .permanentLocation(ItemLocation.builder().build())
      .build();

    when(holdingsClient.getHoldingById(holdingRecordId)).thenReturn(
      HoldingsRecord.builder()
        .effectiveLocationId(effectiveLocationId)
        .callNumber("TK5105.88815 . A58 2004 FT MEADE")
        .build()
    );

    when(locationClient.getLocationById(any())).thenReturn(
      ItemLocation.builder()
        .name("Main Library")
        .build()
    );
    var result = holdingsReferenceService.getEffectiveLocationCallNumberComponentsForItem(item);
    assertEquals("Main Library > TK5105.88815 . A58 2004 FT MEADE", result);
  }

  static Stream<Item> emptyResultMethodSourceArgs(){
    return Stream.of(Item.builder().build(),
      Item.builder()
        .holdingsRecordId(UUID.randomUUID().toString())
        .effectiveLocation(ItemLocation.builder().build())
        .effectiveCallNumberComponents(EffectiveCallNumberComponents.builder().build())
        .build()
    );
  }
}
