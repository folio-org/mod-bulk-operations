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

import lombok.SneakyThrows;
import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.domain.bean.ElectronicAccessRelationship;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.bulkops.exception.NotFoundException;
import org.folio.bulkops.repository.BulkOperationExecutionContentRepository;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ElectronicAccessReferenceService;
import org.folio.bulkops.service.ElectronicAccessService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.util.FolioExecutionContextUtil;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import feign.FeignException;
import org.springframework.boot.test.mock.mockito.SpyBean;

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
  @MockBean
  private ConsortiaService consortiaService;
  @MockBean
  private ElectronicAccessReferenceService electronicAccessReferenceService;
  @SpyBean
  private FolioExecutionContext folioExecutionContext;

  private DataProcessor<ExtendedHoldingsRecord> processor;

  @MockBean
  private BulkOperationExecutionContentRepository bulkOperationExecutionContentRepository;

  @Autowired
  private FolioModuleMetadata folioModuleMetadata;

  @BeforeEach
  void setUp() {
    if (isNull(processor)) {
      processor = factory.getProcessorFromFactory(ExtendedHoldingsRecord.class);
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

    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();

    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(
      new HoldingsRecordsSource()
        .withName("FOLIO")
        .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(locationClient.getLocationById(updatedLocationId)).thenReturn(updatedLocation);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);

    var temporaryLocationUpdatingResult = processor.process(IDENTIFIER, extendedHoldingsRecord, rules(rule(TEMPORARY_LOCATION, REPLACE_WITH, updatedLocationId)));

    assertNotNull(temporaryLocationUpdatingResult);
    assertEquals(updatedLocationId, temporaryLocationUpdatingResult.getUpdated().getEntity().getTemporaryLocationId());
    assertEquals(updatedLocationId, temporaryLocationUpdatingResult.getUpdated().getEntity().getEffectiveLocationId());

    var permanentLocationUpdatingResult = processor.process(IDENTIFIER, extendedHoldingsRecord, rules(rule(PERMANENT_LOCATION, REPLACE_WITH, updatedLocationId)));

    assertNotNull(permanentLocationUpdatingResult);
    assertEquals(updatedLocationId, permanentLocationUpdatingResult.getUpdated().getEntity().getPermanentLocationId());
    assertEquals(temporaryLocationId, permanentLocationUpdatingResult.getUpdated().getEntity().getEffectiveLocationId());
  }

  @Test
  void testUpdateMarcEntity() {
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withSourceId(MARC_SOURCE_ID)).tenantId("tenant").build();

    when(holdingsSourceClient.getById(MARC_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("MARC")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));


    var actual = processor.process(IDENTIFIER, extendedHoldingsRecord, rules(rule(PERMANENT_LOCATION, CLEAR_FIELD, null)));
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
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();

    when(holdingsSourceClient.getById(unknownSourceId)).thenThrow(new NotFoundException("Source was not found"));

    var result = processor.process(IDENTIFIER, extendedHoldingsRecord, rules(rule(TEMPORARY_LOCATION, CLEAR_FIELD, null)));

    assertNotNull(result);
    assertNull(result.getUpdated().getEntity().getTemporaryLocationId());
    assertEquals(permanentLocationId, result.getUpdated().getEntity().getEffectiveLocationId());
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

    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();

    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(
      new HoldingsRecordsSource()
        .withName("FOLIO")
        .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    var result = processor.process(IDENTIFIER, extendedHoldingsRecord, rules(rule(TEMPORARY_LOCATION, CLEAR_FIELD, null)));

    assertNotNull(result);
    assertNull(result.getUpdated().getEntity().getTemporaryLocationId());
    assertEquals(permanentLocationId, result.getUpdated().getEntity().getEffectiveLocationId());
  }

  @Test
  void testClearPermanentLocation() {
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withSourceId(FOLIO_SOURCE_ID)).tenantId("tenant").build();

    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("FOLIO")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    var actual = processor.process(IDENTIFIER, extendedHoldingsRecord, rules(rule(PERMANENT_LOCATION, CLEAR_FIELD, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testReplacePermanentLocationWithEmptyValue() {
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withSourceId(FOLIO_SOURCE_ID)).tenantId("tenant").build();

    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("FOLIO")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    var actual = processor.process(IDENTIFIER, extendedHoldingsRecord, rules(rule(PERMANENT_LOCATION, REPLACE_WITH, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testReplacePermanentLocationWithNonExistedValue() {
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withSourceId(FOLIO_SOURCE_ID)).tenantId("tenant").build();

    var nonExistedLocationId = "62b9c19c-59d0-481d-8957-eb95a96bb144";
    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("FOLIO")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    when(locationClient.getLocationById(nonExistedLocationId))
      .thenThrow(FeignException.FeignClientException.class);

    var actual = processor.process(IDENTIFIER, extendedHoldingsRecord, rules(rule(PERMANENT_LOCATION, REPLACE_WITH, nonExistedLocationId)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testReplacePermanentLocationWithInvalidValue() {
    var invalidLocationId = "62b9c19c-59d0";
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withSourceId(FOLIO_SOURCE_ID)).tenantId("tenant").build();

    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("FOLIO")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    var actual = processor.process(IDENTIFIER, extendedHoldingsRecord, rules(rule(PERMANENT_LOCATION, REPLACE_WITH, invalidLocationId)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.shouldBeUpdated);
  }

  @Test
  void testNonSupportedOptionAndAction() {
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withSourceId(FOLIO_SOURCE_ID)).tenantId("tenant").build();
    var updatedLocationId = "dc3868f6-6169-47b2-88a7-71c2e9e4e924";
    var updatedLocation = new ItemLocation()
      .withId(updatedLocationId)
      .withName("New location");

    when(holdingsSourceClient.getById(FOLIO_SOURCE_ID)).thenReturn(new HoldingsRecordsSource()
      .withName("FOLIO")
      .withSource(HoldingsRecordsSource.SourceEnum.FOLIO));

    when(locationClient.getLocationById(updatedLocationId)).thenReturn(updatedLocation);

    var actual = processor.process(IDENTIFIER, extendedHoldingsRecord, rules(rule(EMAIL_ADDRESS, REPLACE_WITH, updatedLocationId),
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
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holdingsRecord).tenantId("tenant").build();

    var dataProcessor = new HoldingsDataProcessor(null, null, null, null, null, null);

    dataProcessor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_TRUE), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertTrue(holdingsRecord.getDiscoverySuppress());
    dataProcessor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_FALSE), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertFalse(holdingsRecord.getDiscoverySuppress());
    dataProcessor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_TRUE_INCLUDING_ITEMS), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertTrue(holdingsRecord.getDiscoverySuppress());
    dataProcessor.updater(SUPPRESS_FROM_DISCOVERY, new Action().type(SET_TO_FALSE_INCLUDING_ITEMS), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertFalse(holdingsRecord.getDiscoverySuppress());
  }

  @Test
  @SneakyThrows
  void testUpdateMarkAsStaffOnlyForHoldingsNotes() {
    var holdingsNote = new HoldingsNote().withHoldingsNoteTypeId("typeId").withStaffOnly(false);
    var holding= new HoldingsRecord().withNotes(List.of(holdingsNote));

    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();

    var parameter = new Parameter();
    parameter.setKey(HOLDINGS_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(MARK_AS_STAFF_ONLY).parameters(List.of(parameter)), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);

    assertTrue(holding.getNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testUpdateRemoveMarkAsStaffOnlyForHoldingsNotes() {
    var holdingsNote = new HoldingsNote().withHoldingsNoteTypeId("typeId").withStaffOnly(true);
    var  holding= new HoldingsRecord().withNotes(List.of(holdingsNote));

    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();

    var parameter = new Parameter();
    parameter.setKey(HOLDINGS_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId");
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(REMOVE_MARK_AS_STAFF_ONLY).parameters(List.of(parameter)), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);

    assertFalse(holding.getNotes().get(0).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testRemoveAdministrativeNotes() {
    var administrativeNote = "administrative note";
    var holding =  new HoldingsRecord().withAdministrativeNotes(List.of(administrativeNote));
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();

    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(ADMINISTRATIVE_NOTE, new Action().type(REMOVE_ALL), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertTrue(holding.getAdministrativeNotes().isEmpty());
  }

  @Test
  @SneakyThrows
  void testRemoveHoldingsNotes() {
    var note1 = new HoldingsNote().withHoldingsNoteTypeId("typeId1");
    var note2 = new HoldingsNote().withHoldingsNoteTypeId("typeId2");
    var holding = new HoldingsRecord().withNotes(List.of(note1, note2));
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();
    var parameter = new Parameter();
    parameter.setKey(HOLDINGS_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(REMOVE_ALL).parameters(List.of(parameter)), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertEquals(1, holding.getNotes().size());
    assertEquals("typeId2", holding.getNotes().get(0).getHoldingsNoteTypeId());
  }

  @Test
  @SneakyThrows
  void testAddAdministrativeNotes() {
    var administrativeNote1 = "administrative note";
    var administrativeNote2 = "administrative note 2";
    var holding = new HoldingsRecord();
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(ADMINISTRATIVE_NOTE, new Action().type(ADD_TO_EXISTING).updated(administrativeNote1), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertEquals(1, holding.getAdministrativeNotes().size());
    assertEquals(administrativeNote1, holding.getAdministrativeNotes().get(0));

    dataProcessor.updater(ADMINISTRATIVE_NOTE, new Action().type(ADD_TO_EXISTING).updated(administrativeNote2), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertEquals(2, holding.getAdministrativeNotes().size());
  }

  @Test
  @SneakyThrows
  void testAddHoldingsNotes() {
    var note1 = "note1";
    var note2 = "note2";
    var note3 = "note3";
    var holding = new HoldingsRecord();
    var parameter = new Parameter();
    parameter.setKey(HOLDINGS_NOTE_TYPE_ID_KEY);
    parameter.setValue("typeId1");
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(ADD_TO_EXISTING).parameters(List.of(parameter)).updated(note1), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);

    assertEquals(1, holding.getNotes().size());
    assertEquals("typeId1", holding.getNotes().get(0).getHoldingsNoteTypeId());
    assertEquals(note1, holding.getNotes().get(0).getNote());
    assertEquals(false, holding.getNotes().get(0).getStaffOnly());

    parameter.setValue("typeId2");
    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(ADD_TO_EXISTING).parameters(List.of(parameter)).updated(note2), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);

    assertEquals(2, holding.getNotes().size());
    assertEquals("typeId2", holding.getNotes().get(1).getHoldingsNoteTypeId());
    assertEquals(note2, holding.getNotes().get(1).getNote());
    assertEquals(false, holding.getNotes().get(1).getStaffOnly());

    parameter.setValue("typeId3");
    List<Parameter> params = List.of(new Parameter().key(STAFF_ONLY_NOTE_PARAMETER_KEY).value("true"), parameter);
    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(ADD_TO_EXISTING).parameters(params).updated(note3), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertEquals(3, holding.getNotes().size());
    assertEquals("typeId3", holding.getNotes().get(2).getHoldingsNoteTypeId());
    assertEquals(note3, holding.getNotes().get(2).getNote());
    assertEquals(true, holding.getNotes().get(2).getStaffOnly());
  }

  @Test
  @SneakyThrows
  void testFindAndRemoveForAdministrativeNotes() {
    var administrativeNote1 = "administrative note 1";
    var administrativeNote2 = "administrative note 2";
    var holding = new HoldingsRecord().withAdministrativeNotes(new ArrayList<>(List.of(administrativeNote1, administrativeNote2)));
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(ADMINISTRATIVE_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("administrative note"), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertEquals(2, holding.getAdministrativeNotes().size());

    dataProcessor.updater(ADMINISTRATIVE_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial(administrativeNote2), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
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
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("note")
      .parameters(List.of(parameter)), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertEquals(3, holding.getNotes().size());
    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("note1")
      .parameters(List.of(parameter)), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertEquals(2, holding.getNotes().size());
    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(FIND_AND_REMOVE_THESE).initial("note2")
      .parameters(List.of(parameter)), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertEquals(1, holding.getNotes().size());
  }

  @Test
  @SneakyThrows
  void testFindAndReplaceForAdministrativeNotes() {
    var administrativeNote1 = "administrative note 1";
    var administrativeNote2 = "administrative note 2";
    var administrativeNote3 = "administrative note 3";
    var holding = new HoldingsRecord().withAdministrativeNotes(new ArrayList<>(List.of(administrativeNote1, administrativeNote2)));
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(ADMINISTRATIVE_NOTE, new Action().type(FIND_AND_REPLACE)
      .initial(administrativeNote1).updated(administrativeNote3), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
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
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(FIND_AND_REPLACE).parameters(List.of(parameter))
      .initial("note1").updated("note3"), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);

    assertEquals("note3", holding.getNotes().get(0).getNote());
    assertEquals("note1", holding.getNotes().get(1).getNote());
  }

  @Test
  @SneakyThrows
  void testChangeTypeForAdministrativeNotes() {
    var administrativeNote = "note";
    var holding = new HoldingsRecord().withAdministrativeNotes(new ArrayList<>(List.of(administrativeNote)));
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(ADMINISTRATIVE_NOTE, new Action().type(CHANGE_TYPE)
      .updated("typeId"), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
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
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holding).tenantId("tenant").build();
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);

    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(CHANGE_TYPE).updated(ADMINISTRATIVE_NOTE.getValue()).parameters(List.of(parameter)), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);

    assertEquals(1, holding.getAdministrativeNotes().size());
    assertEquals("note1", holding.getAdministrativeNotes().get(0));
    assertEquals(1, holding.getNotes().size());
    assertEquals("note2", holding.getNotes().get(0).getNote());

    holding.setAdministrativeNotes(null);
    holding.setNotes(List.of(note1, note2));

    dataProcessor.updater(HOLDINGS_NOTE, new Action().type(CHANGE_TYPE).updated("typeId3").parameters(List.of(parameter)), extendedHoldingsRecord, false).apply(extendedHoldingsRecord);
    assertEquals(2, holding.getNotes().size());
    assertEquals("note1", holding.getNotes().get(0).getNote());
    assertEquals("typeId3", holding.getNotes().get(0).getHoldingsNoteTypeId());
    assertEquals("note2", holding.getNotes().get(1).getNote());
    assertEquals("typeId2", holding.getNotes().get(1).getHoldingsNoteTypeId());
  }

  @Test
  @SneakyThrows
  void testClone() {
    var dataProcessor = new HoldingsDataProcessor(null, null, new HoldingsNotesUpdater(new AdministrativeNotesUpdater()), null, null, null);
    var administrativeNotes = new ArrayList<String>();
    administrativeNotes.add("note1");
    var holding1 = new HoldingsRecord().withId("id")
      .withAdministrativeNotes(administrativeNotes)
      .withNotes(new ArrayList<>());
    var extendedHoldingsRecord1 = ExtendedHoldingsRecord.builder().entity(holding1).tenantId("tenant").build();
    var extendedHoldingsRecord2 = dataProcessor.clone(extendedHoldingsRecord1);
    assertTrue(dataProcessor.compare(extendedHoldingsRecord1, extendedHoldingsRecord2));

    extendedHoldingsRecord2.getEntity().getAdministrativeNotes().add("note2");
    assertFalse(dataProcessor.compare(extendedHoldingsRecord1, extendedHoldingsRecord2));
    holding1.setAdministrativeNotes(null);

    var note1 = new HoldingsNote().withHoldingsNoteTypeId("typeId").withStaffOnly(true);
    var note2 = new HoldingsNote().withHoldingsNoteTypeId("typeId").withStaffOnly(false);
    holding1.getNotes().add(note1);

    extendedHoldingsRecord2 = dataProcessor.clone(extendedHoldingsRecord1);
    assertTrue(dataProcessor.compare(extendedHoldingsRecord1, extendedHoldingsRecord2));

    extendedHoldingsRecord2.getEntity().getNotes().get(0).setStaffOnly(false);
    assertFalse(dataProcessor.compare(extendedHoldingsRecord1, extendedHoldingsRecord2));

    extendedHoldingsRecord2.getEntity().getNotes().add(note2);

    assertFalse(dataProcessor.compare(extendedHoldingsRecord1, extendedHoldingsRecord2));
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

    var dataProcessor = new HoldingsDataProcessor(null, null, null, new ElectronicAccessUpdaterFactory(folioExecutionContext), null, null);
    dataProcessor.folioExecutionContext = folioExecutionContext;
    var action = new Action().type(CLEAR_FIELD);
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holdingsRecord).tenantId("tenant").build();

    dataProcessor.updater(option, action, extendedHoldingsRecord, false).apply(extendedHoldingsRecord);

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
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holdingsRecord).tenantId("tenant").build();
    var dataProcessor = new HoldingsDataProcessor(null, null, null, new ElectronicAccessUpdaterFactory(folioExecutionContext), null, null);
    dataProcessor.folioExecutionContext = folioExecutionContext;
    var action = new Action().type(FIND_AND_REMOVE_THESE).initial(value);

    dataProcessor.updater(option, action, extendedHoldingsRecord, false).apply(extendedHoldingsRecord);

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
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holdingsRecord).tenantId("tenant").build();
    var dataProcessor = new HoldingsDataProcessor(null, null, null, new ElectronicAccessUpdaterFactory(folioExecutionContext), null, null);
    dataProcessor.folioExecutionContext = folioExecutionContext;
    var action = new Action().type(REPLACE_WITH).updated(newValue);

    dataProcessor.updater(option, action, extendedHoldingsRecord, false).apply(extendedHoldingsRecord);

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
    var extendedHoldingsRecord = ExtendedHoldingsRecord.builder().entity(holdingsRecord).tenantId("tenant").build();
    var dataProcessor = new HoldingsDataProcessor(null, null, null, new ElectronicAccessUpdaterFactory(folioExecutionContext), null, null);
    dataProcessor.folioExecutionContext = folioExecutionContext;
    var action = new Action().type(FIND_AND_REPLACE).initial(initial).updated(updated);

    dataProcessor.updater(option, action, extendedHoldingsRecord, false).apply(extendedHoldingsRecord);

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

  @Test
  void testShouldNotUpdateHoldingWithPermanentLocation_whenLocationFromOtherTenantThanActionTenants() {
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");
    when(consortiaService.getCentralTenantId("memberB")).thenReturn("central");

    try (var ignored = Mockito.mockStatic(FolioExecutionContextUtil.class)) {
      when(FolioExecutionContextUtil.prepareContextForTenant(any(), any(), any())).thenReturn(folioExecutionContext);

      var permLocationFromMemberB = UUID.randomUUID().toString();
      var actionTenants = List.of("memberB");
      var holdId = UUID.randomUUID().toString();
      var initPermLocation = UUID.randomUUID().toString();
      var extendedHolding = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId).withPermanentLocationId(initPermLocation)).tenantId("memberA").build();

      var rules = rules(rule(PERMANENT_LOCATION, REPLACE_WITH, permLocationFromMemberB, actionTenants, List.of()));
      var operationId = rules.getBulkOperationRules().get(0).getBulkOperationId();

      var result = processor.process(IDENTIFIER, extendedHolding, rules);

      assertNotNull(result);
      assertEquals(initPermLocation, result.getUpdated().getEntity().getPermanentLocationId());

      verify(errorService, times(1)).saveError(operationId, IDENTIFIER, String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
        holdId, "memberA", "permanent location").trim());
    }
  }

  @Test
  void testShouldNotUpdateHoldingWithElectronicAccess_whenBothOfRuleAndActionTenantsAreEmpty() {
    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(consortiaService.getCentralTenantId("memberB")).thenReturn("central");
    when(consortiaService.isTenantCentral("central")).thenReturn(true);

    try (var ignored = Mockito.mockStatic(FolioExecutionContextUtil.class)) {
      when(FolioExecutionContextUtil.prepareContextForTenant(any(), any(), any())).thenReturn(folioExecutionContext);

      var electronicAccessFromMemberB = UUID.randomUUID().toString();
      var holdId = UUID.randomUUID().toString();
      var initElectronicAccess = UUID.randomUUID().toString();
      var extendedHolding = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId)
        .withElectronicAccess(List.of(new ElectronicAccess().withRelationshipId(initElectronicAccess)))).tenantId("memberA").build();

      var rules = rules(rule(ELECTRONIC_ACCESS_URL_RELATIONSHIP, FIND_AND_REPLACE, electronicAccessFromMemberB, List.of(), List.of()));
      var operationId = rules.getBulkOperationRules().get(0).getBulkOperationId();

      var result = processor.process(IDENTIFIER, extendedHolding, rules);

      assertNotNull(result);
      assertEquals(initElectronicAccess, result.getUpdated().getEntity().getElectronicAccess().get(0).getRelationshipId());

      verify(errorService, times(1)).saveError(operationId, IDENTIFIER, String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
        holdId, "memberA", "URL relationship").trim());
    }
  }

  @Test
  void testShouldNotUpdateHoldingWithElectronicAccess_whenElectronicAccessIsNotSet() {
    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(consortiaService.getCentralTenantId("memberB")).thenReturn("central");
    when(consortiaService.isTenantCentral("central")).thenReturn(true);

    try (var ignored = Mockito.mockStatic(FolioExecutionContextUtil.class)) {
      when(FolioExecutionContextUtil.prepareContextForTenant(any(), any(), any())).thenReturn(folioExecutionContext);

      var electronicAccessFromMemberB = UUID.randomUUID().toString();
      var holdId = UUID.randomUUID().toString();
      var extendedHolding = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId)).tenantId("memberA").build();

      var rules = rules(rule(ELECTRONIC_ACCESS_URL_RELATIONSHIP, FIND_AND_REPLACE, electronicAccessFromMemberB, List.of(), List.of()));
      var operationId = rules.getBulkOperationRules().get(0).getBulkOperationId();

      var result = processor.process(IDENTIFIER, extendedHolding, rules);

      assertNotNull(result);
      assertNull(result.getUpdated().getEntity().getElectronicAccess());

      verify(errorService, times(1)).saveError(operationId, IDENTIFIER, String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
        holdId, "memberA", "URL relationship").trim());
    }
  }

  @Test
  void testShouldNotUpdateHoldingWithElectronicAccess_whenElectronicAccessIsNotSetAndNonEcs() {
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(consortiaService.getCentralTenantId("diku")).thenReturn("");
    when(consortiaService.isTenantInConsortia("diku")).thenReturn(false);

    try (var ignored = Mockito.mockStatic(FolioExecutionContextUtil.class)) {
      when(FolioExecutionContextUtil.prepareContextForTenant(any(), any(), any())).thenReturn(folioExecutionContext);

      var initElectronicAccForRecord = UUID.randomUUID().toString();
      var initElectronicAccess = UUID.randomUUID().toString();
      var holdId = UUID.randomUUID().toString();
      var extendedHolding = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId)
        .withElectronicAccess(List.of(new ElectronicAccess().withRelationshipId(initElectronicAccForRecord)))).tenantId("diku").build();
      var updatedElectronicAccess = UUID.randomUUID().toString();

      var rules = rules(rule(ELECTRONIC_ACCESS_URL_RELATIONSHIP, FIND_AND_REPLACE, initElectronicAccess, updatedElectronicAccess));

      var result = processor.process(IDENTIFIER, extendedHolding, rules);

      assertNotNull(result);
      assertEquals(initElectronicAccForRecord, result.getUpdated().getEntity().getElectronicAccess().get(0).getRelationshipId());
    }
  }

  @Test
  void testShouldUpdateHoldingWithElectronicAccess_whenElectronicAccessIsSetAndNonEcs() {
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(consortiaService.getCentralTenantId("diku")).thenReturn("");
    when(consortiaService.isTenantInConsortia("diku")).thenReturn(false);
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);

    var initElectronicAccForRecord = UUID.randomUUID().toString();
    var electronicAccessObj = new ElectronicAccess().withRelationshipId(initElectronicAccForRecord);
    var holdId = UUID.randomUUID().toString();
    var extendedHolding = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId)
      .withElectronicAccess(List.of(electronicAccessObj))).tenantId("diku").build();
    var updatedElectronicAccess = UUID.randomUUID().toString();

    when(relationshipClient.getById(updatedElectronicAccess)).thenReturn(new ElectronicAccessRelationship().withId(updatedElectronicAccess));
    when(electronicAccessReferenceService.getRelationshipNameById(updatedElectronicAccess, "diku"))
      .thenReturn("el acc name");

    var rules = rules(rule(ELECTRONIC_ACCESS_URL_RELATIONSHIP, REPLACE_WITH, "", updatedElectronicAccess));

    var result = processor.process(IDENTIFIER, extendedHolding, rules);

    assertNotNull(result);
    verifyNoInteractions(errorService);
    assertEquals(updatedElectronicAccess, result.getUpdated().getEntity().getElectronicAccess().get(0).getRelationshipId());
  }

  @Test
  void testShouldRemoveHoldingWithElectronicAccess_whenElectronicAccessIsSetAndEcs() {
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");
    when(consortiaService.getCentralTenantId("memberB")).thenReturn("central");
    when(consortiaService.isTenantInConsortia("memberB")).thenReturn(true);
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("memberB"));
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);

    var initElectronicAccForRecord = UUID.randomUUID().toString();
    var electronicAccessObj = new ElectronicAccess().withRelationshipId(initElectronicAccForRecord);
    var holdId = UUID.randomUUID().toString();
    var extendedHolding = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId)
      .withElectronicAccess(List.of(electronicAccessObj))).tenantId("memberB").build();

    var rules = rules(new org.folio.bulkops.domain.dto.BulkOperationRule()
      .ruleDetails(new org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails()
        .option(ELECTRONIC_ACCESS_URL_RELATIONSHIP)
        .actions(Collections.singletonList(new Action()
          .type(FIND_AND_REMOVE_THESE)
          .initial(initElectronicAccForRecord)
          .updated("").tenants(List.of("memberB")))).tenants(List.of())));

    var result = processor.process(IDENTIFIER, extendedHolding, rules);

    assertNotNull(result);
    verifyNoInteractions(errorService);
    assertNull(result.getUpdated().getEntity().getElectronicAccess().get(0).getRelationshipId());
  }

  @Test
  void testShouldNotRemoveHoldingWithElectronicAccess_whenNoTenantsProvidedAndHoldingFromDifferentTenant() {
    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(consortiaService.getCentralTenantId("memberB")).thenReturn("central");
    when(consortiaService.isTenantCentral("central")).thenReturn(true);

    var initElectronicAccForRecord = UUID.randomUUID().toString();
    var electronicAccessObj = new ElectronicAccess().withRelationshipId(initElectronicAccForRecord);
    var holdId = UUID.randomUUID().toString();
    var extendedHolding = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId)
      .withElectronicAccess(List.of(electronicAccessObj))).tenantId("memberA").build();

    var rules = rules(new org.folio.bulkops.domain.dto.BulkOperationRule()
      .ruleDetails(new org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails()
        .option(ELECTRONIC_ACCESS_URL_RELATIONSHIP)
        .actions(Collections.singletonList(new Action()
          .type(FIND_AND_REMOVE_THESE)
          .initial(initElectronicAccForRecord)
          .updated("").tenants(List.of()))).tenants(List.of())));
    var operationId = rules.getBulkOperationRules().get(0).getBulkOperationId();

    var result = processor.process(IDENTIFIER, extendedHolding, rules);

    assertNotNull(result);
    verify(errorService, times(1)).saveError(operationId, IDENTIFIER, String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
      holdId, "memberA", "URL relationship").trim());
    assertEquals(initElectronicAccForRecord, result.getUpdated().getEntity().getElectronicAccess().get(0).getRelationshipId());
  }

  @Test
  void testShouldUpdateHoldingWithElectronicAccess_whenElectronicAccessIsSetAndEcs() {
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");
    when(consortiaService.getCentralTenantId("memberB")).thenReturn("central");
    when(consortiaService.isTenantInConsortia("memberB")).thenReturn(true);
    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("memberB"));
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);

    var initElectronicAccForRecord = UUID.randomUUID().toString();
    var electronicAccessObj = new ElectronicAccess().withRelationshipId(initElectronicAccForRecord);
    var holdId = UUID.randomUUID().toString();
    var extendedHolding = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId)
      .withElectronicAccess(List.of(electronicAccessObj))).tenantId("memberB").build();
    var updatedElAcc = initElectronicAccForRecord;

    var rules = rules(new org.folio.bulkops.domain.dto.BulkOperationRule()
      .ruleDetails(new org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails()
        .option(ELECTRONIC_ACCESS_URL_RELATIONSHIP)
        .actions(Collections.singletonList(new Action()
          .type(FIND_AND_REPLACE)
          .initial(initElectronicAccForRecord)
          .updated(updatedElAcc).tenants(List.of()))).tenants(List.of())));

    var result = processor.process(IDENTIFIER, extendedHolding, rules);

    assertNotNull(result);
    verifyNoInteractions(errorService);
    assertEquals(updatedElAcc, result.getUpdated().getEntity().getElectronicAccess().get(0).getRelationshipId());
  }

  @Test
  void testShouldNotUpdateHoldingWithElectronicAccess_whenUpdatedNotExists() {
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(consortiaService.getCentralTenantId("diku")).thenReturn("");
    when(consortiaService.isTenantInConsortia("diku")).thenReturn(false);

    var initElectronicAccForRecord = UUID.randomUUID().toString();
    var electronicAccessObj = new ElectronicAccess().withRelationshipId(initElectronicAccForRecord);
    var holdId = UUID.randomUUID().toString();
    var extendedHolding = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId)
      .withElectronicAccess(List.of(electronicAccessObj))).tenantId("diku").build();
    var updatedElectronicAccess = UUID.randomUUID().toString();

    var rules = rules(rule(ELECTRONIC_ACCESS_URL_RELATIONSHIP, REPLACE_WITH, "", updatedElectronicAccess));
    var operationId = rules.getBulkOperationRules().get(0).getBulkOperationId();

    var result = processor.process(IDENTIFIER, extendedHolding, rules);

    assertNotNull(result);
    verify(errorService, times(1)).saveError(operationId, IDENTIFIER, String.format("URL relationship %s doesn't exist in tenant %s",
      updatedElectronicAccess, "diku").trim());
    assertEquals(initElectronicAccForRecord, result.getUpdated().getEntity().getElectronicAccess().get(0).getRelationshipId());
  }

  @Test
  void testShouldNotUpdateHoldingWithPermanentLocation_whenIntersectionRuleAndActionTenantsGivesNothing() {
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");
    when(consortiaService.getCentralTenantId("memberB")).thenReturn("central");

    try (var ignored = Mockito.mockStatic(FolioExecutionContextUtil.class)) {
      when(FolioExecutionContextUtil.prepareContextForTenant(any(), any(), any())).thenReturn(folioExecutionContext);

      var permLocationFromMemberB = UUID.randomUUID().toString();
      var holdId = UUID.randomUUID().toString();
      var initPermLocation = UUID.randomUUID().toString();
      var extendedHolding = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId).withPermanentLocationId(initPermLocation)).tenantId("memberA").build();

      List<String> actionTenants = new ArrayList<>();
      actionTenants.add("memberA");
      List<String> ruleTenants = new ArrayList<>();
      ruleTenants.add("memberB");
      var rules = rules(rule(PERMANENT_LOCATION, REPLACE_WITH, permLocationFromMemberB, actionTenants, ruleTenants));
      var operationId = rules.getBulkOperationRules().get(0).getBulkOperationId();

      var result = processor.process(IDENTIFIER, extendedHolding, rules);

      assertNotNull(result);
      assertEquals(initPermLocation, result.getUpdated().getEntity().getPermanentLocationId());

      verify(errorService, times(1)).saveError(operationId, IDENTIFIER, String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
        holdId, "memberA", "permanent location").trim());
    }
  }

  @Test
  void testShouldNotUpdateHoldingWithPermanentLocation_whenLocationFromOtherTenantThanRuleTenants() {
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");
    when(consortiaService.getCentralTenantId("memberB")).thenReturn("central");

    try (var ignored = Mockito.mockStatic(FolioExecutionContextUtil.class)) {
      when(FolioExecutionContextUtil.prepareContextForTenant(any(), any(), any())).thenReturn(folioExecutionContext);

      var adminNoteFromMemberB = UUID.randomUUID().toString();
      var ruleTenants = List.of("memberB");
      var holdId = UUID.randomUUID().toString();
      var initPermLocation = UUID.randomUUID().toString();
      var extendedHolding = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId).withPermanentLocationId(initPermLocation)).tenantId("memberA").build();

      var rules = rules(rule(PERMANENT_LOCATION, REPLACE_WITH, adminNoteFromMemberB, List.of(), ruleTenants));
      var operationId = rules.getBulkOperationRules().get(0).getBulkOperationId();

      var result = processor.process(IDENTIFIER, extendedHolding, rules);

      assertNotNull(result);
      assertEquals(initPermLocation, result.getUpdated().getEntity().getPermanentLocationId());

      verify(errorService, times(1)).saveError(operationId, IDENTIFIER, String.format("%s cannot be updated because the record is associated with %s and %s is not associated with this tenant.",
        holdId, "memberA", "permanent location").trim());
    }
  }

  @Test
  void testShouldUpdateHoldingWithLocation_whenLocationFromTenantAmongActionTenants() {

    var locationIdFromMemberB = UUID.randomUUID().toString();

    var actionTenants = List.of("memberB");
    var holdId = UUID.randomUUID().toString();
    var initPermLocation = UUID.randomUUID().toString();
    var extendedHold = ExtendedHoldingsRecord.builder().entity(new HoldingsRecord().withId(holdId).withPermanentLocationId(initPermLocation)).tenantId("memberB").build();

    var rules = rules(rule(PERMANENT_LOCATION, REPLACE_WITH, locationIdFromMemberB, actionTenants, List.of()));

    HashMap<String, Collection<String>> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, List.of("memberB"));
    when(folioExecutionContext.getTenantId()).thenReturn("memberB");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(headers);
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);
    var result = processor.process(IDENTIFIER, extendedHold, rules);

    assertNotNull(result);
    assertEquals(locationIdFromMemberB, result.getUpdated().getEntity().getPermanentLocationId());

    verifyNoInteractions(errorService);
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

