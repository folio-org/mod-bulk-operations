package org.folio.bulkops.processor;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_ALL;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_MARK_AS_STUFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.CHECK_IN_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.CHECK_OUT_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ITEM_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STATUS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOAN_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOCATION;
import static org.folio.bulkops.processor.ItemDataProcessor.ITEM_NOTE_TYPE_ID_KEY;
import static org.folio.bulkops.service.ItemReferenceService.MODULE_NAME;
import static org.folio.bulkops.service.ItemReferenceService.STATUSES_CONFIG_NAME;
import static org.folio.bulkops.util.Constants.BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.CirculationNote;
import org.folio.bulkops.domain.bean.ConfigurationCollection;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.bean.ModelConfiguration;
import org.folio.bulkops.domain.bean.ResultInfo;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.service.ErrorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import lombok.SneakyThrows;

class ItemDataProcessorTest extends BaseTest {

  @Autowired
  DataProcessorFactory factory;
  @MockBean
  ErrorService errorService;

  private DataProcessor<Item> processor;

  @MockBean
  private BulkOperationExecutionContentRepository bulkOperationExecutionContentRepository;

  public static final String IDENTIFIER = "123";

  @BeforeEach
  void setUp() {
    if (isNull(processor)) {
      processor = factory.getProcessorFromFactory(Item.class);
    }
    when(configurationClient.getByQuery(String.format(BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE, MODULE_NAME, STATUSES_CONFIG_NAME)))
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
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
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
    assertNull(result.getUpdated().getPermanentLocation());
    assertNull(result.getUpdated().getTemporaryLocation());
    assertNull(result.getUpdated().getTemporaryLoanType());
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
    assertEquals(updatedLocationId, result.getUpdated().getPermanentLocation().getId());
    assertEquals(updatedLocationId, result.getUpdated().getTemporaryLocation().getId());
    assertEquals(updatedLoanTypeId, result.getUpdated().getPermanentLoanType().getId());
    assertEquals(updatedLoanTypeId, result.getUpdated().getTemporaryLoanType().getId());
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
      assertNull(result.getUpdated().getPermanentLocation());
      assertEquals(temporaryLocation, result.getUpdated().getTemporaryLocation());
      assertEquals(temporaryLocation, result.getUpdated().getEffectiveLocation());
    } else {
      assertNull(result.getUpdated().getTemporaryLocation());
      assertEquals(permanentLocation, result.getUpdated().getPermanentLocation());
      assertEquals(permanentLocation, result.getUpdated().getEffectiveLocation());
    }
  }

  @Test
  @SneakyThrows
  void shouldSetEffectiveLocationBasedOnHoldingsWhenBothLocationsCleared() {
    var holdingsId = UUID.randomUUID().toString();
    var holdingsLocationId = UUID.randomUUID().toString();
    var holdingsLocation = ItemLocation.builder().id(holdingsLocationId).name("Holdings' location").build();

    when(holdingsClient.getHoldingById(holdingsId)).thenReturn(new HoldingsRecord().withPermanentLocationId(holdingsLocationId));
    when(locationClient.getLocationById(holdingsLocationId)).thenReturn(holdingsLocation);

    var item = new Item()
      .withHoldingsRecordId(holdingsId)
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Permanent location"))
      .withTemporaryLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Temporary location"));

    var rules = rules(rule(PERMANENT_LOCATION, CLEAR_FIELD, null),
      rule(TEMPORARY_LOCATION, CLEAR_FIELD, null));

    var result = processor.process(IDENTIFIER, item, rules);

    assertNull(result.getUpdated().getPermanentLocation());
    assertNull(result.getUpdated().getTemporaryLocation());
    assertEquals(holdingsLocation, result.getUpdated().getEffectiveLocation());
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
      assertEquals(newLocation, result.getUpdated().getPermanentLocation());
      if (isNull(temporaryLocation)) {
        assertEquals(newLocation, result.getUpdated().getEffectiveLocation());
      } else {
        assertEquals("temporary", result.getUpdated().getEffectiveLocation().getName());
      }
    } else {
      assertEquals(newLocation, result.getUpdated().getTemporaryLocation());
      assertEquals(newLocation, result.getUpdated().getEffectiveLocation());
    }
  }

  @Test
  void testClearPermanentLoanType() {
    var actual = processor.process(IDENTIFIER, new Item(), rules(rule(PERMANENT_LOAN_TYPE, CLEAR_FIELD, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testReplaceLoanTypeWithEmptyValue() {
    var actual = processor.process(IDENTIFIER, new Item(), rules(rule(PERMANENT_LOAN_TYPE, REPLACE_WITH, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testUpdateAllowedItemStatus() {
    var item = new Item()
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AVAILABLE));

    var rules = rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.MISSING.getValue()));
    var result = processor.process(IDENTIFIER, item, rules);

    assertNotNull(result);
    assertEquals(InventoryItemStatus.NameEnum.MISSING, result.getUpdated().getStatus().getName());
  }

  @Test
  void testUpdateRestrictedItemStatus() {
    var actual = processor.process(IDENTIFIER, new Item()
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AGED_TO_LOST)), rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.MISSING.getValue())));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);

    actual = processor.process(IDENTIFIER, new Item()
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AVAILABLE)), rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.IN_TRANSIT.getValue())));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void shouldNotClearSuppressFromDiscovery() {
    var actual = processor.process(IDENTIFIER, new Item(), rules(rule(SUPPRESS_FROM_DISCOVERY, CLEAR_FIELD, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @ParameterizedTest
  @EnumSource(value = UpdateActionType.class,names = {"SET_TO_FALSE", "SET_TO_TRUE"})
  void shouldUpdateSuppressFromDiscovery(UpdateActionType type) {
    var actual = processor.process(IDENTIFIER, new Item(), rules(rule(SUPPRESS_FROM_DISCOVERY, type, StringUtils.EMPTY)));
    assertNotNull(actual.getUpdated());
    assertTrue(actual.shouldBeUpdated);
  }

  @Test
  @SneakyThrows
  void testUpdateMarkAsStaffOnlyForItemNotes() {
    var itemNote = new ItemNote().withItemNoteTypeId("typeId").withStaffOnly(false);
    var item = new Item().withNotes(List.of(itemNote));
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var processor = new ItemDataProcessor(null, null);

    processor.updater(ITEM_NOTE, new Action().type(MARK_AS_STAFF_ONLY).parameters(List.of(parameter))).apply(item);

    assertTrue(item.getNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testUpdateRemoveMarkAsStaffOnlyForItemNotes() {
    var itemNote = new ItemNote().withItemNoteTypeId("typeId").withStaffOnly(true);
    var item = new Item().withNotes(List.of(itemNote));
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var processor = new ItemDataProcessor(null, null);

    processor.updater(ITEM_NOTE, new Action().type(REMOVE_MARK_AS_STUFF_ONLY).parameters(List.of(parameter))).apply(item);

    assertFalse(item.getNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testUpdateMarkAsStaffOnlyForCirculationNotes() {
    var circulationNote = new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.IN).withStaffOnly(false);
    var item = new Item().withCirculationNotes(List.of(circulationNote));
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var processor = new ItemDataProcessor(null, null);

    processor.updater(CHECK_IN_NOTE, new Action().type(MARK_AS_STAFF_ONLY).parameters(List.of(parameter))).apply(item);
    assertTrue(item.getCirculationNotes().get(0).getStaffOnly());

    circulationNote.setStaffOnly(false);
    circulationNote.setNoteType(CirculationNote.NoteTypeEnum.OUT);

    processor.updater(CHECK_OUT_NOTE, new Action().type(MARK_AS_STAFF_ONLY).parameters(List.of(parameter))).apply(item);
    assertTrue(item.getCirculationNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testUpdateRemoveMarkAsStaffOnlyForCirculationNotes() {
    var circulationNote = new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.IN).withStaffOnly(false);
    var item = new Item().withCirculationNotes(List.of(circulationNote));
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var processor = new ItemDataProcessor(null, null);

    processor.updater(CHECK_IN_NOTE, new Action().type(REMOVE_MARK_AS_STUFF_ONLY).parameters(List.of(parameter))).apply(item);
   assertFalse(item.getCirculationNotes().get(0).getStaffOnly());

    circulationNote.setStaffOnly(true);
    circulationNote.setNoteType(CirculationNote.NoteTypeEnum.OUT);

    processor.updater(CHECK_OUT_NOTE, new Action().type(REMOVE_MARK_AS_STUFF_ONLY).parameters(List.of(parameter))).apply(item);
    assertFalse(item.getCirculationNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testRemoveAdministrativeNotes() {
    var administrativeNote = "administrative note";
    var item = new Item().withAdministrativeNotes(List.of(administrativeNote));
    var processor = new ItemDataProcessor(null, null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(REMOVE_ALL)).apply(item);
    assertTrue(item.getAdministrativeNotes().isEmpty());
  }

  @Test
  @SneakyThrows
  void testRemoveCirculationNotes() {
    var checkInNote = new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.IN);
    var checkOutNote = new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.OUT);
    var item = new Item().withCirculationNotes(List.of(checkInNote, checkOutNote));
    var processor = new ItemDataProcessor(null, null);

    processor.updater(CHECK_IN_NOTE, new Action().type(REMOVE_ALL)).apply(item);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(0).getNoteType());

    item.setCirculationNotes(List.of(checkInNote, checkOutNote));
    processor.updater(CHECK_OUT_NOTE, new Action().type(REMOVE_ALL)).apply(item);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals(CirculationNote.NoteTypeEnum.IN, item.getCirculationNotes().get(0).getNoteType());
  }

  @Test
  @SneakyThrows
  void testRemoveItemNotes() {
    var itemNote1 = new ItemNote().withItemNoteTypeId("typeId1");
    var itemNote2 = new ItemNote().withItemNoteTypeId("typeId2");
    var item = new Item().withNotes(List.of(itemNote1, itemNote2));
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var processor = new ItemDataProcessor(null, null);

    processor.updater(ITEM_NOTE, new Action().type(REMOVE_ALL).parameters(List.of(parameter))).apply(item);
    assertEquals(1, item.getNotes().size());
    assertEquals("typeId2", item.getNotes().get(0).getItemNoteTypeId());
  }

  @Test
  @SneakyThrows
  void testAddAdministrativeNotes() {
    var administrativeNote1 = "administrative note";
    var administrativeNote2 = "administrative note 2";
    var item = new Item();
    var processor = new ItemDataProcessor(null, null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(ADD_TO_EXISTING).updated(administrativeNote1)).apply(item);
    assertEquals(1, item.getAdministrativeNotes().size());
    assertEquals(administrativeNote1, item.getAdministrativeNotes().get(0));

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(ADD_TO_EXISTING).updated(administrativeNote2)).apply(item);
    assertEquals(2, item.getAdministrativeNotes().size());
  }

  @Test
  @SneakyThrows
  void testAddCirculationNotes() {
    var checkInNote = "checkInNote";
    var checkOutNote = "checkOutNote";
    var item = new Item();
    var processor = new ItemDataProcessor(null, null);

    processor.updater(CHECK_IN_NOTE, new Action().type(ADD_TO_EXISTING).updated(checkInNote)).apply(item);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals(checkInNote, item.getCirculationNotes().get(0).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.IN, item.getCirculationNotes().get(0).getNoteType());

    processor.updater(CHECK_OUT_NOTE, new Action().type(ADD_TO_EXISTING).updated(checkOutNote)).apply(item);
    assertEquals(2, item.getCirculationNotes().size());
    assertEquals(checkOutNote, item.getCirculationNotes().get(1).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(1).getNoteType());
  }

  @Test
  @SneakyThrows
  void testAddItemNotes() {
    var itemNote1 = "itemNote1";
    var itemNote2 = "itemNote2";
    var item = new Item();
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");

    var processor = new ItemDataProcessor(null, null);

    processor.updater(ITEM_NOTE, new Action().type(ADD_TO_EXISTING).parameters(List.of(parameter)).updated(itemNote1)).apply(item);

    assertEquals(1, item.getNotes().size());
    assertEquals("typeId1", item.getNotes().get(0).getItemNoteTypeId());
    assertEquals(itemNote1, item.getNotes().get(0).getNote());

    parameter.setValue("typeId2");
    processor.updater(ITEM_NOTE, new Action().type(ADD_TO_EXISTING).parameters(List.of(parameter)).updated(itemNote2)).apply(item);

    assertEquals(2, item.getNotes().size());
    assertEquals("typeId2", item.getNotes().get(1).getItemNoteTypeId());
    assertEquals(itemNote2, item.getNotes().get(1).getNote());
  }
}
