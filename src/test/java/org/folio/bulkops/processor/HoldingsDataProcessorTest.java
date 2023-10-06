package org.folio.bulkops.processor;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE_INCLUDING_ITEMS;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE_INCLUDING_ITEMS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EMAIL_ADDRESS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOCATION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import feign.FeignException;

class HoldingsDataProcessorTest extends BaseTest {

  public static final String FOLIO_SOURCE_ID = "cc38e41b-58ec-4302-b740-21d821020c92";
  public static final String MARC_SOURCE_ID = "58145b85-ef82-4063-8ba0-eb0b892d059e";
  public static final String IDENTIFIER = "678";
  @Autowired
  DataProcessorFactory factory;
  @MockBean
  ErrorService errorService;

  private DataProcessor<HoldingsRecord> processor;

  @MockBean
  private BulkOperationExecutionContentRepository bulkOperationExecutionContentRepository;

  @BeforeEach
  void setUp() {
    if (isNull(processor)) {
      processor = factory.getProcessorFromFactory(HoldingsRecord.class);
    }
    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(
      new HoldingsRecordsSource()
        .withName("FOLIO")
        .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));
    when(holdingsSourceClient.getById(MARC_SOURCE_ID)).thenReturn(
      new HoldingsRecordsSource()
        .withName("MARC")
        .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));
  }

  @Test
  void testReplaceTemporaryLocation() {
    var permanentLocationId = "2508a0cb-e43a-404d-bd78-2e847dfca229";
    var temporaryLocationId = "c8d27cb7-a86b-45f7-b6f4-1604fb467660";
    var updatedLocationId = "dc3868f6-6169-47b2-88a7-71c2e9e4e924";
    var updatedLocation = new ItemLocation()
      .withId(updatedLocationId)
      .withName("New location");

    var holding = new HoldingsRecord()
      .withPermanentLocation(new ItemLocation()
        .withId(permanentLocationId)
        .withName("Permanent Location"))
      .withSourceId(FOLIO_SOURCE_ID)
      .withPermanentLocationId(permanentLocationId)
      .withTemporaryLocationId(temporaryLocationId)
      .withEffectiveLocationId(temporaryLocationId);

    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(
      new HoldingsRecordsSource()
        .withName("FOLIO")
        .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    when(locationClient.getLocationById(updatedLocationId)).thenReturn(updatedLocation);

    var temporaryLocationUpdatingResult = processor.process(IDENTIFIER, holding, rules(rule(TEMPORARY_LOCATION, REPLACE_WITH, updatedLocationId)));

    assertNotNull(temporaryLocationUpdatingResult);
    assertEquals(updatedLocationId, temporaryLocationUpdatingResult.getUpdated().getTemporaryLocationId());
    assertEquals(updatedLocationId, temporaryLocationUpdatingResult.getUpdated().getEffectiveLocationId());

    var permanentLocationUpdatingResult = processor.process(IDENTIFIER, holding, rules(rule(PERMANENT_LOCATION, REPLACE_WITH, updatedLocationId)));

    assertNotNull(permanentLocationUpdatingResult);
    assertEquals(updatedLocation, permanentLocationUpdatingResult.getUpdated().getPermanentLocation());
    assertEquals(updatedLocationId, permanentLocationUpdatingResult.getUpdated().getPermanentLocationId());
    assertEquals(temporaryLocationId, permanentLocationUpdatingResult.getUpdated().getEffectiveLocationId());
  }

  @Test
  void testUpdateMarcEntity() {
    when(holdingsSourceClient.getById(MARC_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("MARC")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    var actual = processor.process(IDENTIFIER, new HoldingsRecord().withSourceId(MARC_SOURCE_ID), rules(rule(PERMANENT_LOCATION, CLEAR_FIELD, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testUpdateHoldingsWithUnknownSource() {
    var permanentLocationId = "2508a0cb-e43a-404d-bd78-2e847dfca229";
    var temporaryLocationId = "c8d27cb7-a86b-45f7-b6f4-1604fb467660";

    var unknownSourceId = UUID.randomUUID().toString();

    var holding = new HoldingsRecord()
      .withPermanentLocation(new ItemLocation()
        .withId(permanentLocationId)
        .withName("Permanent Location"))
      .withSourceId(unknownSourceId)
      .withPermanentLocationId(permanentLocationId)
      .withTemporaryLocationId(temporaryLocationId)
      .withEffectiveLocationId(temporaryLocationId);

    when(holdingsSourceClient.getById(unknownSourceId)).thenThrow(new NotFoundException("Source was not found"));

    var result = processor.process(IDENTIFIER, holding, rules(rule(TEMPORARY_LOCATION, CLEAR_FIELD, null)));

    assertNotNull(result);
    assertNull(result.getUpdated().getTemporaryLocationId());
    assertEquals(permanentLocationId, result.getUpdated().getEffectiveLocationId());
  }

  @Test
  void testClearTemporaryLocation() {
    var permanentLocationId = "2508a0cb-e43a-404d-bd78-2e847dfca229";
    var temporaryLocationId = "c8d27cb7-a86b-45f7-b6f4-1604fb467660";

    var holding = new HoldingsRecord()
      .withPermanentLocation(new ItemLocation()
        .withId(permanentLocationId)
        .withName("Permanent Location"))
      .withSourceId(FOLIO_SOURCE_ID)
      .withPermanentLocationId(permanentLocationId)
      .withTemporaryLocationId(temporaryLocationId)
      .withEffectiveLocationId(temporaryLocationId);

    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(
      new HoldingsRecordsSource()
        .withName("FOLIO")
        .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    var result = processor.process(IDENTIFIER, holding, rules(rule(TEMPORARY_LOCATION, CLEAR_FIELD, null)));

    assertNotNull(result);
    assertNull(result.getUpdated().getTemporaryLocationId());
    assertEquals(permanentLocationId, result.getUpdated().getEffectiveLocationId());
  }

  @Test
  void testClearPermanentLocation() {
    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("FOLIO")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    var actual = processor.process(IDENTIFIER, new HoldingsRecord().withSourceId(FOLIO_SOURCE_ID), rules(rule(PERMANENT_LOCATION, CLEAR_FIELD, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testReplacePermanentLocationWithEmptyValue() {
    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("FOLIO")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    var actual = processor.process(IDENTIFIER, new HoldingsRecord().withSourceId(FOLIO_SOURCE_ID), rules(rule(PERMANENT_LOCATION, REPLACE_WITH, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testReplacePermanentLocationWithNonExistedValue() {
    var nonExistedLocationId = "62b9c19c-59d0-481d-8957-eb95a96bb144";
    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("FOLIO")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    when(locationClient.getLocationById(nonExistedLocationId))
      .thenThrow(FeignException.FeignClientException.class);

    var actual = processor.process(IDENTIFIER, new HoldingsRecord().withSourceId(FOLIO_SOURCE_ID), rules(rule(PERMANENT_LOCATION, REPLACE_WITH, nonExistedLocationId)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);;
  }

  @Test
  void testReplacePermanentLocationWithInvalidValue() {
    var invalidLocationId = "62b9c19c-59d0";

    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("FOLIO")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    var actual = processor.process(IDENTIFIER, new HoldingsRecord().withSourceId(FOLIO_SOURCE_ID), rules(rule(PERMANENT_LOCATION, REPLACE_WITH, invalidLocationId)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testNonSupportedOptionAndAction() {
    var updatedLocationId = "dc3868f6-6169-47b2-88a7-71c2e9e4e924";
    var updatedLocation = new ItemLocation()
      .withId(updatedLocationId)
      .withName("New location");

    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("FOLIO")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    when(locationClient.getLocationById(updatedLocationId)).thenReturn(updatedLocation);

    var actual = processor.process(IDENTIFIER, new HoldingsRecord().withSourceId(FOLIO_SOURCE_ID), rules(rule(EMAIL_ADDRESS, REPLACE_WITH, updatedLocationId),
      rule(PERMANENT_LOCATION, ADD_TO_EXISTING, updatedLocationId)));

    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  @SneakyThrows
  void testUpdaterForSuppressFromDiscoveryOption() {
    var holdingsRecord = new HoldingsRecord()
      .withId(UUID.randomUUID().toString())
      .withDiscoverySuppress(false);

    var processor = new HoldingsDataProcessor(null, null);

    processor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_TRUE)).apply(holdingsRecord);
    assertTrue(holdingsRecord.getDiscoverySuppress());
    processor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_FALSE)).apply(holdingsRecord);
    assertFalse(holdingsRecord.getDiscoverySuppress());
    processor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_TRUE_INCLUDING_ITEMS)).apply(holdingsRecord);
    assertTrue(holdingsRecord.getDiscoverySuppress());
    processor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_FALSE_INCLUDING_ITEMS)).apply(holdingsRecord);
    assertFalse(holdingsRecord.getDiscoverySuppress());
  }
}
