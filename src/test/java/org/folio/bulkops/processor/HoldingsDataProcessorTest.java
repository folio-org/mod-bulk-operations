package org.folio.bulkops.processor;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REMOVE_THESE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE;
import static org.folio.bulkops.domain.dto.UpdateActionType.MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_ALL;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE_INCLUDING_ITEMS;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE_INCLUDING_ITEMS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ELECTRONIC_ACCESS_URL_RELATIONSHIP;
import static org.folio.bulkops.domain.dto.UpdateOptionType.EMAIL_ADDRESS;
import static org.folio.bulkops.domain.dto.UpdateOptionType.HOLDINGS_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.domain.dto.UpdateOptionType.TEMPORARY_LOCATION;
import static org.folio.bulkops.processor.HoldingsNotesUpdater.HOLDINGS_NOTE_TYPE_ID_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.service.ElectronicAccessService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
  @MockBean
  ElectronicAccessService electronicAccessService;

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

    var processor = new HoldingsDataProcessor(null, null, null, null, null);

    processor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_TRUE)).apply(holdingsRecord);
    assertTrue(holdingsRecord.getDiscoverySuppress());
    processor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_FALSE)).apply(holdingsRecord);
    assertFalse(holdingsRecord.getDiscoverySuppress());
    processor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_TRUE_INCLUDING_ITEMS)).apply(holdingsRecord);
    assertTrue(holdingsRecord.getDiscoverySuppress());
    processor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_FALSE_INCLUDING_ITEMS)).apply(holdingsRecord);
    assertFalse(holdingsRecord.getDiscoverySuppress());
  }

  @Test
  @SneakyThrows
  void testUpdateMarkAsStaffOnlyForHoldingsNotes() {
    var holdingsNote = new HoldingsNote().withHoldingsNoteTypeId("typeId").withStaffOnly(false);
    var  holding= new HoldingsRecord().withNotes(List.of(holdingsNote));
    var parameter = new Parameter();
    parameter.setKey(HOLDINGS_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(HOLDINGS_NOTE, new Action().type(MARK_AS_STAFF_ONLY).parameters(List.of(parameter))).apply(holding);

    assertTrue(holding.getNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testUpdateRemoveMarkAsStaffOnlyForHoldingsNotes() {
    var holdingsNote = new HoldingsNote().withHoldingsNoteTypeId("typeId").withStaffOnly(true);
    var  holding= new HoldingsRecord().withNotes(List.of(holdingsNote));
    var parameter = new Parameter();
    parameter.setKey(HOLDINGS_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(HOLDINGS_NOTE, new Action().type(REMOVE_MARK_AS_STAFF_ONLY).parameters(List.of(parameter))).apply(holding);

    assertFalse(holding.getNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testRemoveAdministrativeNotes() {
    var administrativeNote = "administrative note";
    var holding =  new HoldingsRecord().withAdministrativeNotes(List.of(administrativeNote));
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(REMOVE_ALL)).apply(holding);
    assertTrue(holding.getAdministrativeNotes().isEmpty());
  }

  @Test
  @SneakyThrows
  void testRemoveHoldingsNotes() {
    var note1 = new HoldingsNote().withHoldingsNoteTypeId("typeId1");
    var note2 = new HoldingsNote().withHoldingsNoteTypeId("typeId2");
    var holding = new HoldingsRecord().withNotes(List.of(note1, note2));
    var parameter = new Parameter();
    parameter.setKey(HOLDINGS_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(HOLDINGS_NOTE, new Action().type(REMOVE_ALL).parameters(List.of(parameter))).apply(holding);
    assertEquals(1, holding.getNotes().size());
    assertEquals("typeId2", holding.getNotes().get(0).getHoldingsNoteTypeId());
  }

  @Test
  @SneakyThrows
  void testAddAdministrativeNotes() {
    var administrativeNote1 = "administrative note";
    var administrativeNote2 = "administrative note 2";
    var holding = new HoldingsRecord();
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(ADD_TO_EXISTING).updated(administrativeNote1)).apply(holding);
    assertEquals(1, holding.getAdministrativeNotes().size());
    assertEquals(administrativeNote1, holding.getAdministrativeNotes().get(0));

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(ADD_TO_EXISTING).updated(administrativeNote2)).apply(holding);
    assertEquals(2, holding.getAdministrativeNotes().size());
  }

  @Test
  @SneakyThrows
  void testAddHoldingsNotes() {
    var note1 = "note1";
    var note2 = "note2";
    var holding = new HoldingsRecord();
    var parameter = new Parameter();
    parameter.setKey(HOLDINGS_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");

    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(HOLDINGS_NOTE, new Action().type(ADD_TO_EXISTING).parameters(List.of(parameter)).updated(note1)).apply(holding);

    assertEquals(1, holding.getNotes().size());
    assertEquals("typeId1", holding.getNotes().get(0).getHoldingsNoteTypeId());
    assertEquals(note1, holding.getNotes().get(0).getNote());

    parameter.setValue("typeId2");
    processor.updater(HOLDINGS_NOTE, new Action().type(ADD_TO_EXISTING).parameters(List.of(parameter)).updated(note2)).apply(holding);

    assertEquals(2, holding.getNotes().size());
    assertEquals("typeId2", holding.getNotes().get(1).getHoldingsNoteTypeId());
    assertEquals(note2, holding.getNotes().get(1).getNote());
  }

  @Test
  @SneakyThrows
  void testFindAndRemoveForAdministrativeNotes() {
    var administrativeNote1 = "administrative note 1";
    var administrativeNote2 = "administrative note 2";
    var holding = new HoldingsRecord().withAdministrativeNotes(new ArrayList<>(List.of(administrativeNote1, administrativeNote2)));
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("administrative note")).apply(holding);
    assertEquals(2, holding.getAdministrativeNotes().size());

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial(administrativeNote2)).apply(holding);
    assertEquals(1, holding.getAdministrativeNotes().size());
    assertEquals(administrativeNote1, holding.getAdministrativeNotes().get(0));
  }

  @Test
  @SneakyThrows
  void testFindAndRemoveHoldingsNotes() {
    var note1 = new HoldingsNote().withHoldingsNoteTypeId("typeId1").withNote("note1");
    var note2 = new HoldingsNote().withHoldingsNoteTypeId("typeId1").withNote("note2");
    var note3 = new HoldingsNote().withHoldingsNoteTypeId("typeId3").withNote("note1");
    var parameter = new Parameter();
    parameter.setKey(HOLDINGS_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var holding = new HoldingsRecord().withNotes(List.of(note1, note2, note3));
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(HOLDINGS_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("note")
      .parameters(List.of(parameter))).apply(holding);
    assertEquals(3, holding.getNotes().size());
    processor.updater(HOLDINGS_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("note1")
      .parameters(List.of(parameter))).apply(holding);
    assertEquals(2, holding.getNotes().size());
    processor.updater(HOLDINGS_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("note2")
      .parameters(List.of(parameter))).apply(holding);
    assertEquals(1, holding.getNotes().size());
  }

  @Test
  @SneakyThrows
  void testFindAndReplaceForAdministrativeNotes() {
    var administrativeNote1 = "administrative note 1";
    var administrativeNote2 = "administrative note 2";
    var administrativeNote3 = "administrative note 3";
    var holding = new HoldingsRecord().withAdministrativeNotes(new ArrayList<>(List.of(administrativeNote1, administrativeNote2)));
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(FIND_AND_REPLACE)
      .initial(administrativeNote1).updated(administrativeNote3)).apply(holding);
    assertEquals(2, holding.getAdministrativeNotes().size());
    assertEquals(administrativeNote3, holding.getAdministrativeNotes().get(0));
    assertEquals(administrativeNote2, holding.getAdministrativeNotes().get(1));
  }

  @Test
  @SneakyThrows
  void testFindAndReplaceForHoldingNotes() {
    var note1 = new HoldingsNote().withHoldingsNoteTypeId("typeId1").withNote("note1");
    var note2 = new HoldingsNote().withHoldingsNoteTypeId("typeId2").withNote("note1");
    var parameter = new Parameter();
    parameter.setKey(HOLDINGS_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var holding = new HoldingsRecord().withNotes(List.of(note1, note2));
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(HOLDINGS_NOTE, new Action().type(FIND_AND_REPLACE).parameters(List.of(parameter))
      .initial("note1").updated("note3")).apply(holding);

    assertEquals("note3", holding.getNotes().get(0).getNote());
    assertEquals("note1", holding.getNotes().get(1).getNote());
  }

  @Test
  @SneakyThrows
  void testChangeTypeForAdministrativeNotes() {
    var administrativeNote = "note";
    var holding = new HoldingsRecord().withAdministrativeNotes(new ArrayList<>(List.of(administrativeNote)));
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(ADMINISTRATIVE_NOTE, new Action().type(CHANGE_TYPE)
      .updated("typeId")).apply(holding);
    assertEquals(0, holding.getAdministrativeNotes().size());
    assertEquals("note", holding.getNotes().get(0).getNote());
    assertEquals("typeId", holding.getNotes().get(0).getHoldingsNoteTypeId());
  }

  @Test
  @SneakyThrows
  void testChangeNoteTypeForHoldingsNotes() {
    var note1 = new HoldingsNote().withHoldingsNoteTypeId("typeId1").withNote("note1");
    var note2 =  new HoldingsNote().withHoldingsNoteTypeId("typeId2").withNote("note2");
    var parameter = new Parameter();
    parameter.setKey(HOLDINGS_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var holding = new HoldingsRecord().withNotes(List.of(note1, note2));
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);

    processor.updater(HOLDINGS_NOTE, new Action().type(CHANGE_TYPE).updated(ADMINISTRATIVE_NOTE.getValue()).parameters(List.of(parameter))).apply(holding);

    assertEquals(1, holding.getAdministrativeNotes().size());
    assertEquals("note1", holding.getAdministrativeNotes().get(0));
    assertEquals(1, holding.getNotes().size());
    assertEquals("note2", holding.getNotes().get(0).getNote());

    holding.setAdministrativeNotes(null);
    holding.setNotes(List.of(note1, note2));

    processor.updater(HOLDINGS_NOTE, new Action().type(CHANGE_TYPE).updated("typeId3").parameters(List.of(parameter))).apply(holding);
    assertEquals(2, holding.getNotes().size());
    assertEquals("note1", holding.getNotes().get(0).getNote());
    assertEquals("typeId3", holding.getNotes().get(0).getHoldingsNoteTypeId());
    assertEquals("note2", holding.getNotes().get(1).getNote());
    assertEquals("typeId2", holding.getNotes().get(1).getHoldingsNoteTypeId());
  }

  @Test
  @SneakyThrows
  void testClone() {
    var processor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null);
    var administrativeNotes = new ArrayList<String>();
    administrativeNotes.add("note1");
    var holding1 = new HoldingsRecord().withId("id")
      .withAdministrativeNotes(administrativeNotes)
      .withNotes(new ArrayList<>());

    var holding2 = processor.clone(holding1);
    assertTrue(processor.compare(holding1, holding2));

    holding2.getAdministrativeNotes().add("note2");
    assertFalse(processor.compare(holding1, holding2));
    holding1.setAdministrativeNotes(null);

    var note1 = new HoldingsNote().withHoldingsNoteTypeId("typeId").withStaffOnly(true);
    var note2 = new HoldingsNote().withHoldingsNoteTypeId("typeId").withStaffOnly(false);
    holding1.getNotes().add(note1);

    holding2 = processor.clone(holding1);
    assertTrue(processor.compare(holding1, holding2));

    holding2.getNotes().get(0).setStaffOnly(false);
    assertFalse(processor.compare(holding1, holding2));

    holding2.getNotes().add(note2);

    assertFalse(processor.compare(holding1, holding2));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = { "invalid-uuid", "aa76d5d9-4c0f-4895-84f4-56d3266941d1" })
  void shouldNotUpdateInvalidElectronicAccessUrlRelationship(String id) {
    doThrow(new NotFoundException("not found")).when(electronicAccessService).getRelationshipById("aa76d5d9-4c0f-4895-84f4-56d3266941d1");
    var holdingsReferenceService = mock(HoldingsReferenceService.class);
    when(holdingsReferenceService.getSourceById(MARC_SOURCE_ID)).thenReturn(new HoldingsRecordsSource().withName("MARC"));
    var processor = new HoldingsDataProcessor(null, holdingsReferenceService, null, null, electronicAccessService);

    var holdingsRecord = new HoldingsRecord().withSourceId(MARC_SOURCE_ID);
    var action = new Action().type(REPLACE_WITH).updated(id);

    assertThrows(RuleValidationException.class, () -> processor.validator(holdingsRecord).validate(ELECTRONIC_ACCESS_URL_RELATIONSHIP, action));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "ELECTRONIC_ACCESS_URL_RELATIONSHIP",
    "ELECTRONIC_ACCESS_URI",
    "ELECTRONIC_ACCESS_LINK_TEXT",
    "ELECTRONIC_ACCESS_MATERIALS_SPECIFIED",
    "ELECTRONIC_ACCESS_URL_PUBLIC_NOTE"
  })
  @SneakyThrows
  void shouldClearElectronicAccessFields(UpdateOptionType option) {
    var holdingsRecord = buildHoldingsWithElectronicAccess();

    var processor = new HoldingsDataProcessor(null, null, null, new ElectronicAccessUpdaterFactory(), null);
    var action = new Action().type(CLEAR_FIELD);

    processor.updater(option, action).apply(holdingsRecord);

    var electronicAccess = holdingsRecord.getElectronicAccess().get(0);
    switch (option) {
      case ELECTRONIC_ACCESS_URL_RELATIONSHIP -> assertNull(electronicAccess.getRelationshipId());
      case ELECTRONIC_ACCESS_URI -> assertEquals(EMPTY, electronicAccess.getUri());
      case ELECTRONIC_ACCESS_LINK_TEXT -> assertNull(electronicAccess.getLinkText());
      case ELECTRONIC_ACCESS_MATERIALS_SPECIFIED -> assertNull(electronicAccess.getMaterialsSpecification());
      case ELECTRONIC_ACCESS_URL_PUBLIC_NOTE -> assertNull(electronicAccess.getPublicNote());
    }
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
      ELECTRONIC_ACCESS_URL_RELATIONSHIP    | 2510A1D1-A61C-4378-8886-B831004F018E
      ELECTRONIC_ACCESS_URI                 | http://example.org
      ELECTRONIC_ACCESS_LINK_TEXT           | link text
      ELECTRONIC_ACCESS_MATERIALS_SPECIFIED | materials
      ELECTRONIC_ACCESS_URL_PUBLIC_NOTE     | public note
    """, delimiter = '|')
  @SneakyThrows
  void shouldFindAndClearExactlyMatchedElectronicAccessFields(UpdateOptionType option, String value) {
    var holdingsRecord = buildHoldingsWithElectronicAccess();

    var processor = new HoldingsDataProcessor(null, null, null, new ElectronicAccessUpdaterFactory(), null);
    var action = new Action().type(FIND_AND_REMOVE_THESE).initial(value);

    processor.updater(option, action).apply(holdingsRecord);

    var modified = holdingsRecord.getElectronicAccess().get(0);
    var unmodified = holdingsRecord.getElectronicAccess().get(1);
    var initialElectronicAccess = buildHoldingsWithElectronicAccess().getElectronicAccess().get(1);

    switch (option) {
      case ELECTRONIC_ACCESS_URL_RELATIONSHIP -> {
        assertNull(modified.getRelationshipId());
        assertEquals(initialElectronicAccess.getRelationshipId(), unmodified.getRelationshipId());
      }
      case ELECTRONIC_ACCESS_URI -> {
        assertEquals(EMPTY, modified.getUri());
        assertEquals(initialElectronicAccess.getUri(), unmodified.getUri());
      }
      case ELECTRONIC_ACCESS_LINK_TEXT -> {
        assertNull(modified.getLinkText());
        assertEquals(initialElectronicAccess.getLinkText(), unmodified.getLinkText());
      }
      case ELECTRONIC_ACCESS_MATERIALS_SPECIFIED -> {
        assertNull(modified.getMaterialsSpecification());
        assertEquals(initialElectronicAccess.getMaterialsSpecification(), unmodified.getMaterialsSpecification());
      }
      case ELECTRONIC_ACCESS_URL_PUBLIC_NOTE -> {
        assertNull(modified.getPublicNote());
        assertEquals(initialElectronicAccess.getPublicNote(), unmodified.getPublicNote());
      }
    }
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
      ELECTRONIC_ACCESS_URL_RELATIONSHIP    | fc34ddc0-0cfc-40b3-8e1a-bade16b33e5b
      ELECTRONIC_ACCESS_URI                 | http://replaced.org
      ELECTRONIC_ACCESS_LINK_TEXT           | new link text
      ELECTRONIC_ACCESS_MATERIALS_SPECIFIED | new materials
      ELECTRONIC_ACCESS_URL_PUBLIC_NOTE     | new note
    """, delimiter = '|')
  @SneakyThrows
  void shouldReplaceElectronicAccessFields(UpdateOptionType option, String newValue) {
    var holdingsRecord = buildHoldingsWithElectronicAccess();

    var processor = new HoldingsDataProcessor(null, null, null, new ElectronicAccessUpdaterFactory(), null);
    var action = new Action().type(REPLACE_WITH).updated(newValue);

    processor.updater(option, action).apply(holdingsRecord);

    var electronicAccess = holdingsRecord.getElectronicAccess().get(0);
    switch (option) {
      case ELECTRONIC_ACCESS_URL_RELATIONSHIP -> assertEquals(newValue, electronicAccess.getRelationshipId());
      case ELECTRONIC_ACCESS_URI -> assertEquals(newValue, electronicAccess.getUri());
      case ELECTRONIC_ACCESS_LINK_TEXT -> assertEquals(newValue, electronicAccess.getLinkText());
      case ELECTRONIC_ACCESS_MATERIALS_SPECIFIED -> assertEquals(newValue, electronicAccess.getMaterialsSpecification());
      case ELECTRONIC_ACCESS_URL_PUBLIC_NOTE -> assertEquals(newValue, electronicAccess.getPublicNote());
    }
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
      ELECTRONIC_ACCESS_URL_RELATIONSHIP    | 2510a1d1-a61c-4378-8886-b831004f018e  | a6398566-f6cb-4916-96a9-5d3353e06d58
      ELECTRONIC_ACCESS_URI                 | http://example.org                    | http://modified.org
      ELECTRONIC_ACCESS_LINK_TEXT           | link text                             | new link text
      ELECTRONIC_ACCESS_MATERIALS_SPECIFIED | materials                             | new materials
      ELECTRONIC_ACCESS_URL_PUBLIC_NOTE     | public note                           | new public note
    """, delimiter = '|')
  @SneakyThrows
  void shouldFindAndReplaceExactlyMatchedElectronicAccessFields(UpdateOptionType option, String initial, String updated) {
    var holdingsRecord = buildHoldingsWithElectronicAccess();

    var processor = new HoldingsDataProcessor(null, null, null, new ElectronicAccessUpdaterFactory(), null);
    var action = new Action().type(FIND_AND_REPLACE).initial(initial).updated(updated);

    processor.updater(option, action).apply(holdingsRecord);

    var modified = holdingsRecord.getElectronicAccess().get(0);
    var unmodified = holdingsRecord.getElectronicAccess().get(1);
    var initialElectronicAccess = buildHoldingsWithElectronicAccess().getElectronicAccess().get(1);

    switch (option) {
      case ELECTRONIC_ACCESS_URL_RELATIONSHIP -> {
        assertEquals(updated, modified.getRelationshipId());
        assertEquals(initialElectronicAccess.getRelationshipId(), unmodified.getRelationshipId());
      }
      case ELECTRONIC_ACCESS_URI -> {
        assertEquals(updated, modified.getUri());
        assertEquals(initialElectronicAccess.getUri(), unmodified.getUri());
      }
      case ELECTRONIC_ACCESS_LINK_TEXT -> {
        assertEquals(updated, modified.getLinkText());
        assertEquals(initialElectronicAccess.getLinkText(), unmodified.getLinkText());
      }
      case ELECTRONIC_ACCESS_MATERIALS_SPECIFIED -> {
        assertEquals(updated, modified.getMaterialsSpecification());
        assertEquals(initialElectronicAccess.getMaterialsSpecification(), unmodified.getMaterialsSpecification());
      }
      case ELECTRONIC_ACCESS_URL_PUBLIC_NOTE -> {
        assertEquals(updated, modified.getPublicNote());
        assertEquals(initialElectronicAccess.getPublicNote(), unmodified.getPublicNote());
      }
    }
  }

  private HoldingsRecord buildHoldingsWithElectronicAccess() {
    return HoldingsRecord.builder()
      .electronicAccess(List.of(
        ElectronicAccess.builder()
          .relationshipId("2510a1d1-a61c-4378-8886-b831004f018e")
          .uri("http://example.org")
          .linkText("link text")
          .materialsSpecification("materials")
          .publicNote("public note").build(),
        ElectronicAccess.builder()
          .relationshipId("3510a1d1-a61c-4378-8886-b831004f018e")
          .uri("http://example2.org")
          .linkText("Link text")
          .materialsSpecification("Materials")
          .publicNote("note").build()
      )).build();
  }
}

