package org.folio.bulkops.processor;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATUS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOCATION;
import static org.folio.bulkops.processor.ItemDataProcessor.BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE;
import static org.folio.bulkops.processor.ItemDataProcessor.MODULE_NAME;
import static org.folio.bulkops.processor.ItemDataProcessor.STATUSES_CONFIG_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ConfigurationCollection;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.ModelConfiguration;
import org.folio.bulkops.domain.bean.ResultInfo;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ItemDataProcessorTest extends BaseTest {

  @Autowired
  DataProcessorFactory factory;

  private DataProcessor<Item> processor;

  @MockBean
  private BulkOperationExecutionContentRepository bulkOperationExecutionContentRepository;

  public static final String IDENTIFIER = "123";

  @BeforeEach
  void setUp() {
    if (isNull(processor)) {
      processor = factory.getProcessorFromFactory(Item.class);
    }
    when(configurationClient.getConfigurations(String.format(BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE, MODULE_NAME, STATUSES_CONFIG_NAME)))
      .thenReturn(
        new ConfigurationCollection()
          .withConfigs(List.of(new ModelConfiguration()
            .withId("6e2fcd41-3d6e-40e7-871d-4ae2bd494a59")
            .withModule("BULKEDIT")
            .withConfigName("statuses")
            .with_default(true)
            .withEnabled(true)
            .withValue("{\"Available\":[\"Missing\"],\"Missing\":[\"Withdrawn\"]}")))
          .withTotalRecords(1)
          .withResultInfo(new ResultInfo()
            .withTotalRecords(1)));
  }

  @Test
  void testClearItemStatus() {
    var actual = processor.process(IDENTIFIER, new Item(), rules(rule(STATUS, CLEAR_FIELD, null)));
    assertNotNull(actual.getEntity());
    assertFalse(actual.isChanged);
  }

  @Test
  void testClearItemLocationAndLoanType() {
    var item = new Item()
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Permanent location"))
      .withTemporaryLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Temporary location"))
      .withPermanentLoanType(new LoanType().withId(UUID.randomUUID().toString()).withName("Permanent loan type"));

    var rules = rules(rule(PERMANENT_LOCATION, CLEAR_FIELD, null),
      rule(TEMPORARY_LOCATION, CLEAR_FIELD, null), rule(TEMPORARY_LOAN_TYPE, CLEAR_FIELD, null));

    var result = processor.process(IDENTIFIER, item, rules);
    assertNotNull(result);
    assertNull(result.getEntity().getPermanentLocation());
    assertNull(result.getEntity().getTemporaryLocation());
    assertNull(result.getEntity().getTemporaryLoanType());
  }

  @Test
  void testUpdateItemAndLoanTypeLocation() {
    var updatedLocationId = "dc3868f6-6169-47b2-88a7-71c2e9e4e924";
    var updatedLocation = new ItemLocation()
      .withId(updatedLocationId)
      .withName("New location");

    var updatedLoanTypeId = "2c2857f6-381a-4782-9385-3524ebef8b69";
    var updatedLoanType = new LoanType()
      .withId(updatedLoanTypeId)
      .withName("New loan type");

    when(locationClient.getLocationById(updatedLocationId)).thenReturn(updatedLocation);
    when(loanTypeClient.getLoanTypeById(updatedLoanTypeId)).thenReturn(updatedLoanType);

    var item = new Item()
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Permanent location"))
      .withTemporaryLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Temporary location"))
      .withPermanentLoanType(new LoanType().withId(UUID.randomUUID().toString()).withName("Permanent loan type"))
      .withTemporaryLoanType(new LoanType().withId(UUID.randomUUID().toString()).withName("Temporary loan type"));

    var rules = rules(rule(PERMANENT_LOCATION, REPLACE_WITH, updatedLocationId),
      rule(TEMPORARY_LOCATION, REPLACE_WITH, updatedLocationId), rule(PERMANENT_LOAN_TYPE, REPLACE_WITH, updatedLoanTypeId),
      rule(TEMPORARY_LOAN_TYPE, REPLACE_WITH, updatedLoanTypeId));

    var result = processor.process(IDENTIFIER, item, rules);

    assertNotNull(result);
    assertEquals(updatedLocationId, result.getEntity().getPermanentLocation().getId());
    assertEquals(updatedLocationId, result.getEntity().getTemporaryLocation().getId());
    assertEquals(updatedLoanTypeId, result.getEntity().getPermanentLoanType().getId());
    assertEquals(updatedLoanTypeId, result.getEntity().getTemporaryLoanType().getId());
  }

  @ParameterizedTest
  @ValueSource(strings = { "PERMANENT_LOCATION", "TEMPORARY_LOCATION" })
  void shouldUpdateItemEffectiveLocationOnClearLocation(UpdateOptionType optionType) {
    var permanentLocation = new ItemLocation().withId(UUID.randomUUID().toString()).withName("Permanent location");
    var temporaryLocation = new ItemLocation().withId(UUID.randomUUID().toString()).withName("Temporary location");
    var item = new Item()
      .withPermanentLocation(permanentLocation)
      .withTemporaryLocation(temporaryLocation);

    var rules = rules(rule(optionType, CLEAR_FIELD, null));

    var result = processor.process(IDENTIFIER, item, rules);

    if (PERMANENT_LOCATION.equals(optionType)) {
      assertNull(result.getEntity().getPermanentLocation());
      assertEquals(temporaryLocation, result.getEntity().getTemporaryLocation());
      assertEquals(temporaryLocation, result.getEntity().getEffectiveLocation());
    } else {
      assertNull(result.getEntity().getTemporaryLocation());
      assertEquals(permanentLocation, result.getEntity().getPermanentLocation());
      assertEquals(permanentLocation, result.getEntity().getEffectiveLocation());
    }
  }

  @Test
  @SneakyThrows
  void shouldSetEffectiveLocationBasedOnHoldingsWhenBothLocationsCleared() {
    var holdingsId = UUID.randomUUID().toString();
    var holdingsLocationId = UUID.randomUUID().toString();
    var holdingsLocation = ItemLocation.builder().id(holdingsLocationId).name("Holdings' location").build();

    when(holdingsClient.getHoldingById(holdingsId)).thenReturn(OBJECT_MAPPER.readTree(String.format("{ \"permanentLocationId\": \"%s\" }", holdingsLocationId)));
    when(locationClient.getLocationById(holdingsLocationId)).thenReturn(holdingsLocation);

    var item = new Item()
      .withHoldingsRecordId(holdingsId)
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Permanent location"))
      .withTemporaryLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Temporary location"));

    var rules = rules(rule(PERMANENT_LOCATION, CLEAR_FIELD, null),
      rule(TEMPORARY_LOCATION, CLEAR_FIELD, null));

    var result = processor.process(IDENTIFIER, item, rules);

    assertNull(result.getEntity().getPermanentLocation());
    assertNull(result.getEntity().getTemporaryLocation());
    assertEquals(holdingsLocation, result.getEntity().getEffectiveLocation());
  }

  @ParameterizedTest
  @CsvSource(value = { ",,PERMANENT_LOCATION", ",,TEMPORARY_LOCATION",
    "p,,PERMANENT_LOCATION", "p,,TEMPORARY_LOCATION",
    ",t,PERMANENT_LOCATION", ",t,TEMPORARY_LOCATION",
    "p,t,PERMANENT_LOCATION", "p,t,TEMPORARY_LOCATION"}, delimiter = ',')
  @SneakyThrows
  void shouldUpdateItemEffectiveLocationOnClear(String permanentLocation, String temporaryLocation, UpdateOptionType optionType) {
    var newLocationId = UUID.randomUUID().toString();
    var newLocation = ItemLocation.builder().id(newLocationId).name("new location").build();
    var item = Item.builder()
      .permanentLocation(isNull(permanentLocation) ? null : ItemLocation.builder().id(UUID.randomUUID().toString()).name("permanent").build())
      .temporaryLocation(isNull(temporaryLocation) ? null : ItemLocation.builder().id(UUID.randomUUID().toString()).name("temporary").build())
      .build();

    when(locationClient.getLocationById(newLocationId)).thenReturn(newLocation);

    var rules = rules(rule(optionType, REPLACE_WITH, newLocationId));

    var result = processor.process(IDENTIFIER, item, rules);

    assertNotNull(result);
    if (PERMANENT_LOCATION.equals(optionType)) {
      assertEquals(newLocation, result.getEntity().getPermanentLocation());
      if (isNull(temporaryLocation)) {
        assertEquals(newLocation, result.getEntity().getEffectiveLocation());
      } else {
        assertEquals("temporary", result.getEntity().getEffectiveLocation().getName());
      }
    } else {
      assertEquals(newLocation, result.getEntity().getTemporaryLocation());
      assertEquals(newLocation, result.getEntity().getEffectiveLocation());
    }
  }

  @Test
  void testClearPermanentLoanType() {
    var actual = processor.process(IDENTIFIER, new Item(), rules(rule(PERMANENT_LOAN_TYPE, CLEAR_FIELD, null)));
    assertNotNull(actual.getEntity());
    assertFalse(actual.isChanged);
  }

  @Test
  void testReplaceLoanTypeWithEmptyValue() {
    var actual = processor.process(IDENTIFIER, new Item(), rules(rule(PERMANENT_LOAN_TYPE, REPLACE_WITH, null)));
    assertNotNull(actual.getEntity());
    assertFalse(actual.isChanged);
  }

  @Test
  void testUpdateAllowedItemStatus() {
    var item = new Item()
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AVAILABLE));

    var rules = rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.MISSING.getValue()));
    var result = processor.process(IDENTIFIER, item, rules);

    assertNotNull(result);
    assertEquals(InventoryItemStatus.NameEnum.MISSING, result.getEntity().getStatus().getName());
    assertNotNull(result.getEntity().getStatus().getDate());
  }

  @Test
  void testUpdateRestrictedItemStatus() {
    var actual = processor.process(IDENTIFIER, new Item()
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AGED_TO_LOST)), rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.MISSING.getValue())));
    assertNotNull(actual.getEntity());
    assertFalse(actual.isChanged);

    actual = processor.process(IDENTIFIER, new Item()
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AVAILABLE)), rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.IN_TRANSIT.getValue())));
    assertNotNull(actual.getEntity());
    assertFalse(actual.isChanged);
  }
}
