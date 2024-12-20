package org.folio.bulkops.processor;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.bean.InventoryItemStatus.NameEnum.AVAILABLE;
import static org.folio.bulkops.domain.bean.InventoryItemStatus.NameEnum.MISSING;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.DUPLICATE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REMOVE_THESE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE;
import static org.folio.bulkops.domain.dto.UpdateActionType.MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_ALL;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
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
import static org.folio.bulkops.processor.folio.ItemsNotesUpdater.ADMINISTRATIVE_NOTE_TYPE;
import static org.folio.bulkops.processor.folio.ItemsNotesUpdater.CHECK_IN_NOTE_TYPE;
import static org.folio.bulkops.processor.folio.ItemsNotesUpdater.CHECK_OUT_NOTE_TYPE;
import static org.folio.bulkops.processor.folio.ItemsNotesUpdater.ITEM_NOTE_TYPE_ID_KEY;
import static org.folio.bulkops.util.Constants.STAFF_ONLY_NOTE_PARAMETER_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.CirculationNote;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.InventoryItemStatus;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.bean.ItemNote;
import org.folio.bulkops.domain.bean.LoanType;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.processor.folio.AdministrativeNotesUpdater;
import org.folio.bulkops.processor.folio.DataProcessorFactory;
import org.folio.bulkops.processor.folio.ItemDataProcessor;
import org.folio.bulkops.processor.folio.ItemsNotesUpdater;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.ItemReferenceService;
import org.folio.bulkops.util.FolioExecutionContextUtil;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import lombok.SneakyThrows;
import org.springframework.boot.test.mock.mockito.SpyBean;

class ItemDataProcessorTest extends BaseTest {

  @Autowired
  DataProcessorFactory factory;
  @MockBean
  ErrorService errorService;
  @MockBean
  private HoldingsReferenceService holdingsReferenceService;
  @MockBean
  private ItemReferenceService itemReferenceService;
  @SpyBean
  private FolioExecutionContext folioExecutionContext;
  @MockBean
  private ConsortiaService consortiaService;

  private FolioDataProcessor<ExtendedItem> processor;

  @MockBean
  private BulkOperationExecutionContentRepository bulkOperationExecutionContentRepository;

  public static final String IDENTIFIER = "123";

  @BeforeEach
  void setUp() {
    if (isNull(processor)) {
      processor = factory.getProcessorFromFactory(ExtendedItem.class);
    }
    when(itemReferenceService.getAllowedStatuses(AVAILABLE.getValue()))
      .thenReturn(Collections.singletonList(MISSING.getValue()));
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("tenant"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);
    when(folioExecutionContext.getTenantId()).thenReturn("tenant");
  }

  @Test
  void testClearItemStatus() {
    var actual = processor.process(IDENTIFIER, ExtendedItem.builder().entity(new Item()).build(), rules(rule(STATUS, CLEAR_FIELD, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testClearItemLocationAndLoanType() {
    var holdingsId = UUID.randomUUID().toString();
    var locationId = UUID.randomUUID().toString();
    when(holdingsReferenceService.getHoldingsRecordById(holdingsId, "tenant"))
      .thenReturn(new HoldingsRecord().withPermanentLocationId(locationId));
    when(itemReferenceService.getLocationById(locationId, "tenant"))
      .thenReturn(new ItemLocation().withId(locationId));
    var item = new Item()
      .withHoldingsRecordId(holdingsId)
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Permanent location"))
      .withTemporaryLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Temporary location"))
      .withPermanentLoanType(new LoanType().withId(UUID.randomUUID().toString()).withName("Permanent loan type"));

    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();

    var rules = rules(rule(PERMANENT_LOCATION, CLEAR_FIELD, null),
      rule(TEMPORARY_LOCATION, CLEAR_FIELD, null), rule(TEMPORARY_LOAN_TYPE, CLEAR_FIELD, null));

    var result = processor.process(IDENTIFIER, extendedItem, rules);
    assertNotNull(result);
    assertNull(result.getUpdated().getEntity().getPermanentLocation());
    assertNull(result.getUpdated().getEntity().getTemporaryLocation());
    assertNull(result.getUpdated().getEntity().getTemporaryLoanType());
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

    when(itemReferenceService.getLocationById(updatedLocationId, "tenant")).thenReturn(updatedLocation);
    when(itemReferenceService.getLoanTypeById(updatedLoanTypeId, "tenant")).thenReturn(updatedLoanType);

    var item = new Item()
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Permanent location"))
      .withTemporaryLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Temporary location"))
      .withPermanentLoanType(new LoanType().withId(UUID.randomUUID().toString()).withName("Permanent loan type"))
      .withTemporaryLoanType(new LoanType().withId(UUID.randomUUID().toString()).withName("Temporary loan type"));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();

    var rules = rules(rule(PERMANENT_LOCATION, REPLACE_WITH, updatedLocationId),
      rule(TEMPORARY_LOCATION, REPLACE_WITH, updatedLocationId), rule(PERMANENT_LOAN_TYPE, REPLACE_WITH, updatedLoanTypeId),
      rule(TEMPORARY_LOAN_TYPE, REPLACE_WITH, updatedLoanTypeId));

    var result = processor.process(IDENTIFIER, extendedItem, rules);

    assertNotNull(result);
    assertEquals(updatedLocationId, result.getUpdated().getEntity().getPermanentLocation().getId());
    assertEquals(updatedLocationId, result.getUpdated().getEntity().getTemporaryLocation().getId());
    assertEquals(updatedLoanTypeId, result.getUpdated().getEntity().getPermanentLoanType().getId());
    assertEquals(updatedLoanTypeId, result.getUpdated().getEntity().getTemporaryLoanType().getId());
  }

  @ParameterizedTest
  @ValueSource(strings = { "PERMANENT_LOCATION", "TEMPORARY_LOCATION" })
  void shouldUpdateItemEffectiveLocationOnClearLocation(UpdateOptionType optionType) {
    var permanentLocation = new ItemLocation().withId(UUID.randomUUID().toString()).withName("Permanent location");
    var temporaryLocation = new ItemLocation().withId(UUID.randomUUID().toString()).withName("Temporary location");
    var item = new Item()
      .withPermanentLocation(permanentLocation)
      .withTemporaryLocation(temporaryLocation);
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();

    var rules = rules(rule(optionType, CLEAR_FIELD, null));

    var result = processor.process(IDENTIFIER, extendedItem, rules);

    if (PERMANENT_LOCATION.equals(optionType)) {
      assertNull(result.getUpdated().getEntity().getPermanentLocation());
      assertEquals(temporaryLocation, result.getUpdated().getEntity().getTemporaryLocation());
      assertEquals(temporaryLocation, result.getUpdated().getEntity().getEffectiveLocation());
    } else {
      assertNull(result.getUpdated().getEntity().getTemporaryLocation());
      assertEquals(permanentLocation, result.getUpdated().getEntity().getPermanentLocation());
      assertEquals(permanentLocation, result.getUpdated().getEntity().getEffectiveLocation());
    }
  }

  @Test
  @SneakyThrows
  void shouldSetEffectiveLocationBasedOnHoldingsWhenBothLocationsCleared() {
    var holdingsId = UUID.randomUUID().toString();
    var holdingsLocationId = UUID.randomUUID().toString();
    var holdingsLocation = ItemLocation.builder().id(holdingsLocationId).name("Holdings' location").build();

    when(holdingsReferenceService.getHoldingsRecordById(holdingsId, "tenant")).thenReturn(new HoldingsRecord().withPermanentLocationId(holdingsLocationId));
    when(itemReferenceService.getLocationById(holdingsLocationId, "tenant")).thenReturn(holdingsLocation);

    var item = new Item()
      .withHoldingsRecordId(holdingsId)
      .withPermanentLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Permanent location"))
      .withTemporaryLocation(new ItemLocation().withId(UUID.randomUUID().toString()).withName("Temporary location"));

    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var rules = rules(rule(PERMANENT_LOCATION, CLEAR_FIELD, null),
      rule(TEMPORARY_LOCATION, CLEAR_FIELD, null));

    var result = processor.process(IDENTIFIER, extendedItem, rules);

    assertNull(result.getUpdated().getEntity().getPermanentLocation());
    assertNull(result.getUpdated().getEntity().getTemporaryLocation());
    assertEquals(holdingsLocation, result.getUpdated().getEntity().getEffectiveLocation());
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
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();

    when(itemReferenceService.getLocationById(newLocationId, "tenant"))
      .thenReturn(newLocation);

    var rules = rules(rule(optionType, REPLACE_WITH, newLocationId));

    var result = processor.process(IDENTIFIER, extendedItem, rules);

    assertNotNull(result);
    if (PERMANENT_LOCATION.equals(optionType)) {
      assertEquals(newLocation, result.getUpdated().getEntity().getPermanentLocation());
      if (isNull(temporaryLocation)) {
        assertEquals(newLocation, result.getUpdated().getEntity().getEffectiveLocation());
      } else {
        assertEquals("temporary", result.getUpdated().getEntity().getEffectiveLocation().getName());
      }
    } else {
      assertEquals(newLocation, result.getUpdated().getEntity().getTemporaryLocation());
      assertEquals(newLocation, result.getUpdated().getEntity().getEffectiveLocation());
    }
  }

  @Test
  void testClearPermanentLoanType() {
    var extendedItem = ExtendedItem.builder().entity(new Item()).tenantId("tenant").build();
    var actual = processor.process(IDENTIFIER,  extendedItem, rules(rule(PERMANENT_LOAN_TYPE, CLEAR_FIELD, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testReplaceLoanTypeWithEmptyValue() {
    var extendedItem = ExtendedItem.builder().entity(new Item()).tenantId("tenant").build();
    var actual = processor.process(IDENTIFIER, extendedItem, rules(rule(PERMANENT_LOAN_TYPE, REPLACE_WITH, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testUpdateAllowedItemStatus(boolean isMember) {
    var item = new Item()
      .withStatus(new InventoryItemStatus().withName(AVAILABLE));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant_" + isMember).build();

    when(consortiaService.isTenantMember(any())).thenReturn(isMember);

    var rules = rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.MISSING.getValue()));
    var result = processor.process(IDENTIFIER, extendedItem, rules);

    assertNotNull(result);
    assertEquals(InventoryItemStatus.NameEnum.MISSING, result.getUpdated().getEntity().getStatus().getName());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testUpdateRestrictedItemStatus(boolean isMember) {
    var extendedItem = ExtendedItem.builder().entity(new Item()
      .withStatus(new InventoryItemStatus().withName(InventoryItemStatus.NameEnum.AGED_TO_LOST))).tenantId("tenant").build();

    when(consortiaService.isTenantMember(any())).thenReturn(isMember);

    var actual = processor.process(IDENTIFIER, extendedItem, rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.MISSING.getValue())));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);

    actual = processor.process(IDENTIFIER, ExtendedItem.builder().entity(new Item()
      .withStatus(new InventoryItemStatus().withName(AVAILABLE))).tenantId("tenant").build(), rules(rule(STATUS, REPLACE_WITH, InventoryItemStatus.NameEnum.IN_TRANSIT.getValue())));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void shouldNotClearSuppressFromDiscovery() {
    var extendedItem = ExtendedItem.builder().entity(new Item()).tenantId("tenant").build();

    var actual = processor.process(IDENTIFIER, extendedItem, rules(rule(SUPPRESS_FROM_DISCOVERY, CLEAR_FIELD, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @ParameterizedTest
  @EnumSource(value = UpdateActionType.class,names = {"SET_TO_FALSE", "SET_TO_TRUE"})
  void shouldUpdateSuppressFromDiscovery(UpdateActionType type) {
    var extendedItem = ExtendedItem.builder().entity(new Item()).tenantId("tenant").build();

    var actual = processor.process(IDENTIFIER, extendedItem, rules(rule(SUPPRESS_FROM_DISCOVERY, type, StringUtils.EMPTY)));
    assertNotNull(actual.getUpdated());
    var expectedDiscoverySuppress = SET_TO_TRUE.equals(type);
    assertEquals(expectedDiscoverySuppress, actual.getUpdated().getEntity().getDiscoverySuppress());
  }

  @Test
  @SneakyThrows
  void testUpdateMarkAsStaffOnlyForItemNotes() {
    var itemNote = new ItemNote().withItemNoteTypeId("typeId").withStaffOnly(false);
    var item = new Item().withNotes(List.of(itemNote));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ITEM_NOTE, new Action().type(MARK_AS_STAFF_ONLY).parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);

    assertTrue(item.getNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testUpdateRemoveMarkAsStaffOnlyForItemNotes() {
    var itemNote = new ItemNote().withItemNoteTypeId("typeId").withStaffOnly(true);
    var item = new Item().withNotes(List.of(itemNote));
    var parameter = new Parameter();
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ITEM_NOTE, new Action().type(REMOVE_MARK_AS_STAFF_ONLY).parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);

    assertFalse(item.getNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testUpdateMarkAsStaffOnlyForCirculationNotes() {
    var circulationNote = new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.IN).withStaffOnly(false);
    var item = new Item().withCirculationNotes(List.of(circulationNote));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(CHECK_IN_NOTE, new Action().type(MARK_AS_STAFF_ONLY).parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);
    assertTrue(item.getCirculationNotes().get(0).getStaffOnly());

    circulationNote.setStaffOnly(false);
    circulationNote.setNoteType(CirculationNote.NoteTypeEnum.OUT);

    processor.updater(CHECK_OUT_NOTE, new Action().type(MARK_AS_STAFF_ONLY).parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);
    assertTrue(item.getCirculationNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testUpdateRemoveMarkAsStaffOnlyForCirculationNotes() {
    var circulationNote = new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.IN).withStaffOnly(false);
    var item = new Item().withCirculationNotes(List.of(circulationNote));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(CHECK_IN_NOTE, new Action().type(REMOVE_MARK_AS_STAFF_ONLY).parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);
    assertFalse(item.getCirculationNotes().get(0).getStaffOnly());

    circulationNote.setStaffOnly(true);
    circulationNote.setNoteType(CirculationNote.NoteTypeEnum.OUT);

    processor.updater(CHECK_OUT_NOTE, new Action().type(REMOVE_MARK_AS_STAFF_ONLY).parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);
    assertFalse(item.getCirculationNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testRemoveAdministrativeNotes() {
    var administrativeNote = "administrative note";
    var item = new Item().withAdministrativeNotes(List.of(administrativeNote));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(REMOVE_ALL), extendedItem, false).apply(extendedItem);
    assertTrue(item.getAdministrativeNotes().isEmpty());
  }

  @Test
  @SneakyThrows
  void testRemoveCirculationNotes() {
    var checkInNote = new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.IN);
    var checkOutNote = new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.OUT);
    var item = new Item().withCirculationNotes(List.of(checkInNote, checkOutNote));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(CHECK_IN_NOTE, new Action().type(REMOVE_ALL), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(0).getNoteType());

    item.setCirculationNotes(List.of(checkInNote, checkOutNote));
    processor.updater(CHECK_OUT_NOTE, new Action().type(REMOVE_ALL), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals(CirculationNote.NoteTypeEnum.IN, item.getCirculationNotes().get(0).getNoteType());
  }

  @Test
  @SneakyThrows
  void testRemoveCheckInNoteAndAddCheckOutNoteOfTheSameNoteType() {
    var checkInNote = new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.IN);
    var item = new Item().withCirculationNotes(List.of(checkInNote));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(CHECK_IN_NOTE, new Action().type(REMOVE_ALL), extendedItem, false).apply(extendedItem);
    processor.updater(CHECK_OUT_NOTE, new Action().type(ADD_TO_EXISTING), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(0).getNoteType());
  }

  @Test
  @SneakyThrows
  void testRemoveItemNoteAndAddItemNoteOfTheSameNoteType() {
    var actionNote = new ItemNote().withItemNoteTypeId("typeId1").withNote("Action note");
    var item = new Item().withNotes(List.of(actionNote));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ITEM_NOTE, new Action().type(FIND_AND_REMOVE_THESE).parameters(List.of(parameter)).initial("Action note"), extendedItem, false).apply(extendedItem);
    processor.updater(ITEM_NOTE, new Action().type(ADD_TO_EXISTING).parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getNotes().size());
    assertEquals("typeId1", item.getNotes().get(0).getItemNoteTypeId());
  }

  @Test
  @SneakyThrows
  void testRemoveItemNotes() {
    var itemNote1 = new ItemNote().withItemNoteTypeId("typeId1");
    var itemNote2 = new ItemNote().withItemNoteTypeId("typeId2");
    var item = new Item().withNotes(List.of(itemNote1, itemNote2));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ITEM_NOTE, new Action().type(REMOVE_ALL).parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getNotes().size());
    assertEquals("typeId2", item.getNotes().get(0).getItemNoteTypeId());
  }

  @Test
  @SneakyThrows
  void testAddAdministrativeNotes() {
    var administrativeNote1 = "administrative note";
    var administrativeNote2 = "administrative note 2";
    var item = new Item();
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(ADD_TO_EXISTING).updated(administrativeNote1), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getAdministrativeNotes().size());
    assertEquals(administrativeNote1, item.getAdministrativeNotes().get(0));

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(ADD_TO_EXISTING).updated(administrativeNote2), extendedItem, false).apply(extendedItem);
    assertEquals(2, item.getAdministrativeNotes().size());
  }

  @Test
  @SneakyThrows
  void testAddCirculationNotes() {
    var checkInNote = "checkInNote";
    var checkOutNote = "checkOutNote";
    var item = new Item();
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(CHECK_IN_NOTE, new Action().type(ADD_TO_EXISTING).updated(checkInNote), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals(checkInNote, item.getCirculationNotes().get(0).getNote());
    assertEquals(false, item.getCirculationNotes().get(0).getStaffOnly());
    assertEquals(CirculationNote.NoteTypeEnum.IN, item.getCirculationNotes().get(0).getNoteType());

    processor.updater(CHECK_OUT_NOTE, new Action().type(ADD_TO_EXISTING).updated(checkOutNote), extendedItem, false).apply(extendedItem);
    assertEquals(2, item.getCirculationNotes().size());
    assertEquals(checkOutNote, item.getCirculationNotes().get(1).getNote());
    assertEquals(false, item.getCirculationNotes().get(1).getStaffOnly());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(1).getNoteType());

    List<Parameter> params = Collections.singletonList(new Parameter().key(STAFF_ONLY_NOTE_PARAMETER_KEY).value("true"));
    processor.updater(CHECK_OUT_NOTE, new Action().type(ADD_TO_EXISTING).parameters(params).updated(checkOutNote), extendedItem, false).apply(extendedItem);
    assertEquals(3, item.getCirculationNotes().size());
    assertEquals(checkOutNote, item.getCirculationNotes().get(2).getNote());
    assertEquals(true, item.getCirculationNotes().get(2).getStaffOnly());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(2).getNoteType());
  }

  @Test
  @SneakyThrows
  void testAddItemNotes() {
    var itemNote1 = "itemNote1";
    var itemNote2 = "itemNote2";
    var item = new Item();
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");

    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ITEM_NOTE, new Action().type(ADD_TO_EXISTING).parameters(List.of(parameter)).updated(itemNote1), extendedItem, false).apply(extendedItem);

    assertEquals(1, item.getNotes().size());
    assertEquals("typeId1", item.getNotes().get(0).getItemNoteTypeId());
    assertEquals(itemNote1, item.getNotes().get(0).getNote());

    parameter.setValue("typeId2");
    processor.updater(ITEM_NOTE, new Action().type(ADD_TO_EXISTING).parameters(List.of(parameter)).updated(itemNote2), extendedItem, false).apply(extendedItem);

    assertEquals(2, item.getNotes().size());
    assertEquals("typeId2", item.getNotes().get(1).getItemNoteTypeId());
    assertEquals(itemNote2, item.getNotes().get(1).getNote());
  }

  @Test
  @SneakyThrows
  void testFindAndRemoveForAdministrativeNotes() {
    var administrativeNote1 = "administrative note";
    var administrativeNote2 = "Administrative note";
    var item = new Item().withAdministrativeNotes(new ArrayList<>(List.of(administrativeNote1, administrativeNote2)));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("administrative"), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getAdministrativeNotes().size());
    assertEquals("Administrative note", item.getAdministrativeNotes().get(0));

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("Administrative"), extendedItem, false).apply(extendedItem);
    assertTrue(item.getAdministrativeNotes().isEmpty());
  }

  @Test
  @SneakyThrows
  void testFindAndRemoveForCirculationNotes() {
    var checkInNote = new CirculationNote()
      .withNoteType(CirculationNote.NoteTypeEnum.IN).withNote("circ note");
    var checkOutNote = new CirculationNote()
      .withNoteType(CirculationNote.NoteTypeEnum.OUT).withNote("circ note");
    var item = new Item().withCirculationNotes(List.of(checkInNote, checkOutNote));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(CHECK_OUT_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("note"), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getCirculationNotes().size());
    processor.updater(CHECK_OUT_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("circ note"), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals("circ note", item.getCirculationNotes().get(0).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.IN, item.getCirculationNotes().get(0).getNoteType());

    item.setCirculationNotes(List.of(checkInNote, checkOutNote));
    processor.updater(CHECK_IN_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("circ note"), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals("circ note", item.getCirculationNotes().get(0).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(0).getNoteType());
  }

  @Test
  @SneakyThrows
  void testFindAndRemoveItemNotes() {
    var itemNote1 = new ItemNote().withItemNoteTypeId("typeId1").withNote("itemNote1");
    var itemNote2 = new ItemNote().withItemNoteTypeId("typeId1").withNote("itemNote2");
    var itemNote3 = new ItemNote().withItemNoteTypeId("typeId3").withNote("itemNote1");
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var item = new Item().withNotes(List.of(itemNote1, itemNote2, itemNote3));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ITEM_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("Note2")
      .parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);
    assertEquals(2, item.getNotes().size());
    assertEquals("itemNote1", item.getNotes().get(0).getNote());
    assertEquals("typeId1", item.getNotes().get(0).getItemNoteTypeId());
    assertEquals("itemNote1", item.getNotes().get(1).getNote());
    assertEquals("typeId3", item.getNotes().get(1).getItemNoteTypeId());
  }

  @Test
  @SneakyThrows
  void testFindAndReplaceForAdministrativeNotes() {
    var administrativeNote1 = "administrative note";
    var administrativeNote2 = "Administrative note";
    var item = new Item().withAdministrativeNotes(new ArrayList<>(List.of(administrativeNote1, administrativeNote2)));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(FIND_AND_REPLACE)
      .initial("administrative").updated("new administrative"), extendedItem, false).apply(extendedItem);
    assertEquals(2, item.getAdministrativeNotes().size());
    assertEquals("new administrative note", item.getAdministrativeNotes().get(0));
    assertEquals("Administrative note", item.getAdministrativeNotes().get(1));
  }

  @Test
  @SneakyThrows
  void testFindAndReplaceForCirculationNotes() {
    var checkInNote = new CirculationNote()
      .withNoteType(CirculationNote.NoteTypeEnum.IN).withNote("check-in note");
    var checkOutNote = new CirculationNote()
      .withNoteType(CirculationNote.NoteTypeEnum.OUT).withNote("check-out note");
    var item = new Item().withCirculationNotes(List.of(checkInNote, checkOutNote));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(CHECK_IN_NOTE, new Action().type(FIND_AND_REPLACE)
      .initial("check-in").updated("new check-in"), extendedItem, false).apply(extendedItem);
    assertEquals(2, item.getCirculationNotes().size());
    assertEquals("new check-in note", item.getCirculationNotes().get(0).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.IN, item.getCirculationNotes().get(0).getNoteType());
    assertEquals("check-out note", item.getCirculationNotes().get(1).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(1).getNoteType());

    checkInNote.setNote("check-in note");

    processor.updater(CHECK_OUT_NOTE, new Action().type(FIND_AND_REPLACE)
      .initial("check-out").updated("new check-out"), extendedItem, false).apply(extendedItem);
    assertEquals(2, item.getCirculationNotes().size());
    assertEquals("check-in note", item.getCirculationNotes().get(0).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.IN, item.getCirculationNotes().get(0).getNoteType());
    assertEquals("new check-out note", item.getCirculationNotes().get(1).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(1).getNoteType());
  }

  @Test
  @SneakyThrows
  void testFindAndReplaceForItemNotes() {
    var itemNote1 = new ItemNote().withItemNoteTypeId("typeId1").withNote("itemNote1");
    var itemNote2 = new ItemNote().withItemNoteTypeId("typeId2").withNote("itemNote1");
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var item = new Item().withNotes(List.of(itemNote1, itemNote2));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ITEM_NOTE, new Action().type(FIND_AND_REPLACE).parameters(List.of(parameter))
      .initial("itemNote1").updated("itemNote3"), extendedItem, false).apply(extendedItem);

    assertEquals("itemNote3", item.getNotes().get(0).getNote());
    assertEquals("itemNote1", item.getNotes().get(1).getNote());
  }

  @Test
  @SneakyThrows
  void testChangeTypeForAdministrativeNotes() {
    var administrativeNote = "note";
    var item = new Item().withAdministrativeNotes(new ArrayList<>(List.of(administrativeNote)));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(CHANGE_TYPE)
      .updated(CHECK_IN_NOTE_TYPE), extendedItem, false).apply(extendedItem);
    assertEquals(0, item.getAdministrativeNotes().size());
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals("note", item.getCirculationNotes().get(0).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.IN, item.getCirculationNotes().get(0).getNoteType());

    item.setCirculationNotes(null);
    item.setAdministrativeNotes(List.of(administrativeNote));

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(CHANGE_TYPE)
      .updated(CHECK_OUT_NOTE_TYPE), extendedItem, false).apply(extendedItem);
    assertEquals(0, item.getAdministrativeNotes().size());
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals("note", item.getCirculationNotes().get(0).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(0).getNoteType());

    item.setCirculationNotes(null);
    item.setAdministrativeNotes(List.of(administrativeNote));

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(CHANGE_TYPE)
      .updated("typeId"), extendedItem, false).apply(extendedItem);

    assertEquals(0, item.getAdministrativeNotes().size());
    assertEquals(1, item.getNotes().size());
    assertEquals("note", item.getNotes().get(0).getNote());
    assertEquals("typeId", item.getNotes().get(0).getItemNoteTypeId());
  }

  @Test
  @SneakyThrows
  void testChangeNoteTypeForCirculationNotes() {
    var checkInNote = new CirculationNote()
      .withNoteType(CirculationNote.NoteTypeEnum.IN).withNote("note").withStaffOnly(true);
    var checkOutNote = new CirculationNote()
      .withNoteType(CirculationNote.NoteTypeEnum.OUT).withNote("note 2").withStaffOnly(true);
    var item = new Item().withCirculationNotes(List.of(checkInNote, checkOutNote));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(CHECK_IN_NOTE, new Action().type(CHANGE_TYPE)
      .updated(CHECK_OUT_NOTE_TYPE), extendedItem, false).apply(extendedItem);
    assertEquals(2, item.getCirculationNotes().size());
    assertEquals("note", item.getCirculationNotes().get(0).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(0).getNoteType());
    assertTrue(item.getCirculationNotes().get(0).getStaffOnly());

    checkInNote.setNoteType(CirculationNote.NoteTypeEnum.IN);

    processor.updater(CHECK_IN_NOTE, new Action().type(CHANGE_TYPE)
      .updated(ADMINISTRATIVE_NOTE_TYPE), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(0).getNoteType());
    assertEquals(1, item.getAdministrativeNotes().size());
    assertEquals("note", item.getAdministrativeNotes().get(0));

    item.setAdministrativeNotes(null);
    item.setCirculationNotes(List.of(checkInNote, checkOutNote));

    processor.updater(CHECK_IN_NOTE, new Action().type(CHANGE_TYPE)
      .updated("typeId"), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(0).getNoteType());
    assertEquals(1, item.getNotes().size());
    assertEquals("note", item.getNotes().get(0).getNote());
    assertEquals("typeId", item.getNotes().get(0).getItemNoteTypeId());
    assertTrue(item.getNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testChangeNoteTypeForItemNotes() {
    var itemNote1 = new ItemNote().withItemNoteTypeId("typeId1").withNote("itemNote1").withStaffOnly(true);
    var itemNote2 = new ItemNote().withItemNoteTypeId("typeId2").withNote("itemNote2");
    var parameter = new Parameter();
    parameter.setKey(ITEM_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var item = new Item().withNotes(List.of(itemNote1, itemNote2));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(ITEM_NOTE, new Action().type(CHANGE_TYPE).updated(ADMINISTRATIVE_NOTE_TYPE).parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);

    assertEquals(1, item.getAdministrativeNotes().size());
    assertEquals("itemNote1", item.getAdministrativeNotes().get(0));
    assertEquals(1, item.getNotes().size());
    assertEquals("itemNote2", item.getNotes().get(0).getNote());

    item.setAdministrativeNotes(null);
    item.setNotes(List.of(itemNote1, itemNote2));

    processor.updater(ITEM_NOTE, new Action().type(CHANGE_TYPE).updated(CHECK_IN_NOTE_TYPE).parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals("itemNote1", item.getCirculationNotes().get(0).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.IN, item.getCirculationNotes().get(0).getNoteType());
    assertTrue(item.getCirculationNotes().get(0).getStaffOnly());
    assertEquals(1, item.getNotes().size());
    assertEquals("itemNote2", item.getNotes().get(0).getNote());

    item.setCirculationNotes(null);
    item.setNotes(List.of(itemNote1, itemNote2));

    processor.updater(ITEM_NOTE, new Action().type(CHANGE_TYPE).updated(CHECK_OUT_NOTE_TYPE).parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);
    assertEquals(1, item.getCirculationNotes().size());
    assertEquals("itemNote1", item.getCirculationNotes().get(0).getNote());
    assertEquals(CirculationNote.NoteTypeEnum.OUT, item.getCirculationNotes().get(0).getNoteType());
    assertTrue(item.getCirculationNotes().get(0).getStaffOnly());
    assertEquals(1, item.getNotes().size());
    assertEquals("itemNote2", item.getNotes().get(0).getNote());

    item.setCirculationNotes(null);
    item.setNotes(List.of(itemNote1, itemNote2));

    processor.updater(ITEM_NOTE, new Action().type(CHANGE_TYPE).updated("typeId3").parameters(List.of(parameter)), extendedItem, false).apply(extendedItem);
    assertEquals(2, item.getNotes().size());
    assertEquals("itemNote1", item.getNotes().get(0).getNote());
    assertEquals("typeId3", item.getNotes().get(0).getItemNoteTypeId());
    assertEquals("itemNote2", item.getNotes().get(1).getNote());
    assertEquals("typeId2", item.getNotes().get(1).getItemNoteTypeId());
 }

  @Test
  @SneakyThrows
  void testDuplicateForCirculationNotes() {
    var checkInNote = new CirculationNote().withId(UUID.randomUUID().toString())
      .withNoteType(CirculationNote.NoteTypeEnum.IN).withNote("note 1").withStaffOnly(true);
    var checkOutNote = new CirculationNote().withId(UUID.randomUUID().toString())
      .withNoteType(CirculationNote.NoteTypeEnum.OUT).withNote("note 2").withStaffOnly(true);
    var item = new Item().withCirculationNotes(new ArrayList<>(List.of(checkInNote, checkOutNote)));
    var extendedItem = ExtendedItem.builder().entity(item).tenantId("tenant").build();
    var processor = new ItemDataProcessor(null, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);

    processor.updater(CHECK_IN_NOTE, new Action().type(DUPLICATE).updated(CHECK_OUT_NOTE_TYPE), extendedItem, false).apply(extendedItem);
    assertEquals(3, item.getCirculationNotes().size());
    assertEquals(2, item.getCirculationNotes().stream().filter(circNote -> circNote.getNoteType() == CirculationNote.NoteTypeEnum.OUT).count());
    var duplicated = item.getCirculationNotes().stream().filter(circNote ->
      circNote.getNoteType() == CirculationNote.NoteTypeEnum.OUT && StringUtils.equals(circNote.getNote(), "note 1")).findFirst();
    assertTrue(duplicated.isPresent());
    assertTrue(duplicated.get().getStaffOnly());

    processor.updater(CHECK_OUT_NOTE, new Action().type(DUPLICATE).updated(CHECK_IN_NOTE_TYPE), extendedItem, false).apply(extendedItem);
    assertEquals(5, item.getCirculationNotes().size());
    assertEquals(3, item.getCirculationNotes().stream().filter(circNote -> circNote.getNoteType() == CirculationNote.NoteTypeEnum.IN).count());

    long count = item.getCirculationNotes().stream().filter(circNote ->
      circNote.getNoteType() == CirculationNote.NoteTypeEnum.IN && StringUtils.equals(circNote.getNote(), "note 1")).count();
    assertEquals(2, count);

    duplicated = item.getCirculationNotes().stream().filter(circNote ->
      circNote.getNoteType() == CirculationNote.NoteTypeEnum.IN && StringUtils.equals(circNote.getNote(), "note 2")).findFirst();
    assertTrue(duplicated.isPresent());
    assertTrue(duplicated.get().getStaffOnly());
  }

  @Test
  void testClone() {
    var processor = new ItemDataProcessor(holdingsReferenceService, null, new ItemsNotesUpdater(new AdministrativeNotesUpdater()), null);
    var administrativeNotes = new ArrayList<String>();
    administrativeNotes.add("note1");
    var item1 = new Item().withId("id")
      .withAdministrativeNotes(administrativeNotes)
      .withCirculationNotes(new ArrayList<>())
      .withNotes(new ArrayList<>());
    var extendedItem1 = ExtendedItem.builder().entity(item1).tenantId("tenant").build();

    var extendedItem2 = processor.clone(extendedItem1);
    assertTrue(processor.compare(extendedItem1, extendedItem2));

    extendedItem2.getEntity().getAdministrativeNotes().add("note2");
    assertFalse(processor.compare(extendedItem1, extendedItem2));
    item1.setAdministrativeNotes(null);

    var checkInNote = new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.IN);
    var checkOutNote = new CirculationNote().withNoteType(CirculationNote.NoteTypeEnum.OUT);
    item1.getCirculationNotes().add(checkInNote);

    extendedItem2 = processor.clone(extendedItem1);
    assertTrue(processor.compare(extendedItem1, extendedItem2));

    extendedItem2.getEntity().getCirculationNotes().get(0).setNoteType(CirculationNote.NoteTypeEnum.OUT);
    assertFalse(processor.compare(extendedItem1, extendedItem2));

    extendedItem2.getEntity().getCirculationNotes().add(checkOutNote);
    assertFalse(processor.compare(extendedItem1, extendedItem2));

    item1.setCirculationNotes(null);

    var itemNote1 = new ItemNote().withItemNoteTypeId("typeId").withStaffOnly(true);
    var itemNote2 = new ItemNote().withItemNoteTypeId("typeId").withStaffOnly(false);
    item1.getNotes().add(itemNote1);

    extendedItem2 = processor.clone(extendedItem1);
    assertTrue(processor.compare(extendedItem1, extendedItem2));

    extendedItem2.getEntity().getNotes().get(0).setStaffOnly(false);
    assertFalse(processor.compare(extendedItem1, extendedItem2));

    extendedItem2.getEntity().getNotes().add(itemNote2);

    assertFalse(processor.compare(extendedItem1, extendedItem2));
  }

  @Test
  void testShouldNotUpdateItemWithLoanType_whenLoanTypeFromOtherTenantThanActionTenants() {
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");
    when(consortiaService.getCentralTenantId("memberB")).thenReturn("central");

    try (var ignored = Mockito.mockStatic(FolioExecutionContextUtil.class)) {
      when(FolioExecutionContextUtil.prepareContextForTenant(any(), any(), any())).thenReturn(folioExecutionContext);

      var loanTypeFromMemberB = UUID.randomUUID().toString();
      var actionTenants = List.of("memberB");
      var itemId = UUID.randomUUID().toString();
      var initPermanentLoanTypeId = UUID.randomUUID().toString();
      var extendedItem = ExtendedItem.builder().entity(new Item().withId(itemId).withPermanentLoanType(new LoanType().withId(initPermanentLoanTypeId))).tenantId("memberA").build();

      var rules = rules(rule(PERMANENT_LOAN_TYPE, REPLACE_WITH, loanTypeFromMemberB, actionTenants, List.of()));
      var operationId = rules.getBulkOperationRules().get(0).getBulkOperationId();

      var result = processor.process(IDENTIFIER, extendedItem, rules);

      assertNotNull(result);
      assertEquals(initPermanentLoanTypeId, result.getUpdated().getEntity().getPermanentLoanType().getId());

      verify(errorService, times(1)).saveError(operationId, IDENTIFIER, String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
        itemId, "memberA", "permanent loan type").trim());
    }
  }

  @Test
  void testShouldNotUpdateItemWithLoanType_whenLoanTypeFromOtherTenantThanRuleTenants() {
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");
    when(consortiaService.getCentralTenantId("memberB")).thenReturn("central");

    try (var ignored = Mockito.mockStatic(FolioExecutionContextUtil.class)) {
      when(FolioExecutionContextUtil.prepareContextForTenant(any(), any(), any())).thenReturn(folioExecutionContext);

      var adminNoteFromMemberB = UUID.randomUUID().toString();
      var ruleTenants = List.of("memberB");
      var itemId = UUID.randomUUID().toString();
      var initPermanentLoanTypeId = UUID.randomUUID().toString();
      var extendedItem = ExtendedItem.builder().entity(new Item().withId(itemId).withPermanentLoanType(new LoanType().withId(initPermanentLoanTypeId))).tenantId("memberA").build();

      var rules = rules(rule(PERMANENT_LOAN_TYPE, REPLACE_WITH, adminNoteFromMemberB, List.of(), ruleTenants));
      var operationId = rules.getBulkOperationRules().get(0).getBulkOperationId();

      var result = processor.process(IDENTIFIER, extendedItem, rules);

      assertNotNull(result);
      assertEquals(initPermanentLoanTypeId, result.getUpdated().getEntity().getPermanentLoanType().getId());

      verify(errorService, times(1)).saveError(operationId, IDENTIFIER, String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
        itemId, "memberA", "permanent loan type").trim());
    }
  }

  @Test
  void testShouldUpdateItemWithLoanType_whenLoanTypeFromTenantAmongRuleTenants() {

    var permanentLoanTypeFromMemberB = UUID.randomUUID().toString();

    when(loanTypeClient.getLoanTypeById(permanentLoanTypeFromMemberB)).thenReturn(new LoanType().withId(permanentLoanTypeFromMemberB));
    when(itemReferenceService.getLoanTypeById(permanentLoanTypeFromMemberB, "memberB")).thenReturn(new LoanType().withId(permanentLoanTypeFromMemberB));
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");

    var ruleTenants = List.of("memberB", "memberA");
    var itemId = UUID.randomUUID().toString();
    var initPermanentLoanTypeId = UUID.randomUUID().toString();
    var extendedItem = ExtendedItem.builder().entity(new Item().withId(itemId).withPermanentLoanType(new LoanType().withId(initPermanentLoanTypeId))).tenantId("memberA").build();

    var rules = rules(rule(PERMANENT_LOAN_TYPE, REPLACE_WITH, permanentLoanTypeFromMemberB, List.of(), ruleTenants));

    var result = processor.process(IDENTIFIER, extendedItem, rules);

    assertNotNull(result);
    assertEquals(permanentLoanTypeFromMemberB, result.getUpdated().getEntity().getPermanentLoanType().getId());

    verifyNoInteractions(errorService);
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
      HOLDINGS_NOTE                      | note type
      ITEM_NOTE                          | note type
      PERMANENT_LOAN_TYPE                | permanent loan type
      TEMPORARY_LOAN_TYPE                | temporary loan type
      PERMANENT_LOCATION                 | permanent location
      TEMPORARY_LOCATION                 | temporary location
      ELECTRONIC_ACCESS_URL_RELATIONSHIP | URL relationship
    """, delimiter = '|')
  void testGetRecordPropertyName(UpdateOptionType optionType, String returnValue) {
    var dataProcessor = (FolioAbstractDataProcessor)processor;
    assertEquals(returnValue, dataProcessor.getRecordPropertyName(optionType));
  }

  @Test
  void testShouldNotUpdateHoldingWithPermanentLoanType_whenIntersectionRuleAndActionTenantsGivesNothing() {
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");
    when(consortiaService.getCentralTenantId("memberB")).thenReturn("central");

    try (var ignored = Mockito.mockStatic(FolioExecutionContextUtil.class)) {
      when(FolioExecutionContextUtil.prepareContextForTenant(any(), any(), any())).thenReturn(folioExecutionContext);

      var permLocationFromMemberB = UUID.randomUUID().toString();
      var itemId = UUID.randomUUID().toString();
      var initPermLocation = UUID.randomUUID().toString();
      var extendedHolding = ExtendedItem.builder().entity(new Item().withId(itemId).withPermanentLocation(
        new ItemLocation().withId(initPermLocation))).tenantId("memberA").build();

      List<String> actionTenants = new ArrayList<>();
      actionTenants.add("memberA");
      List<String> ruleTenants = new ArrayList<>();
      ruleTenants.add("memberB");
      var rules = rules(rule(PERMANENT_LOAN_TYPE, REPLACE_WITH, permLocationFromMemberB, actionTenants, ruleTenants));
      var operationId = rules.getBulkOperationRules().get(0).getBulkOperationId();

      var result = processor.process(IDENTIFIER, extendedHolding, rules);

      assertNotNull(result);
      assertEquals(initPermLocation, result.getUpdated().getEntity().getPermanentLocation().getId());

      verify(errorService, times(1)).saveError(operationId, IDENTIFIER, String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
        itemId, "memberA", "permanent loan type").trim());
    }
  }
}
