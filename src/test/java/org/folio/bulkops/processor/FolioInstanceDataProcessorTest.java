package org.folio.bulkops.processor;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.bulkops.domain.dto.UpdateActionType.ADD_TO_EXISTING;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateActionType.CLEAR_FIELD;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REMOVE_THESE;
import static org.folio.bulkops.domain.dto.UpdateActionType.FIND_AND_REPLACE;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_ALL;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_MARK_AS_STAFF_ONLY;
import static org.folio.bulkops.domain.dto.UpdateActionType.REMOVE_SOME;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_FALSE;
import static org.folio.bulkops.domain.dto.UpdateActionType.SET_TO_TRUE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.INSTANCE_NOTE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.STAFF_SUPPRESS;
import static org.folio.bulkops.processor.folio.InstanceNotesUpdaterFactory.INSTANCE_NOTE_TYPE_ID_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.InstanceNote;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.RuleDetails;
import org.folio.bulkops.exception.RuleValidationException;
import org.folio.bulkops.processor.folio.AdministrativeNotesUpdater;
import org.folio.bulkops.processor.folio.DataProcessorFactory;
import org.folio.bulkops.processor.folio.FolioInstanceDataProcessor;
import org.folio.bulkops.processor.folio.InstanceNotesUpdaterFactory;
import org.folio.bulkops.processor.folio.StatisticalCodesUpdater;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

class FolioInstanceDataProcessorTest extends BaseTest {
  @Autowired
  DataProcessorFactory factory;
   @MockitoBean
  ErrorService errorService;
   @MockitoBean
  private ConsortiaService consortiaService;

  private FolioDataProcessor<ExtendedInstance> processor;

  public static final String IDENTIFIER = "123";


  @BeforeEach
  void setUp() {
    when(consortiaService.isTenantMember(any(String.class))).thenReturn(false);
    if (isNull(processor)) {
      processor = factory.getProcessorFromFactory(ExtendedInstance.class);
    }
  }

  @Test
  void testSetStaffSuppressToTrue() {
    var extendedInstance = ExtendedInstance.builder().entity(new Instance()).build();
    var actual = processor.process(IDENTIFIER, extendedInstance, rules(rule(STAFF_SUPPRESS, SET_TO_TRUE, null)));
    assertNotNull(actual.getUpdated());
    assertTrue(actual.getUpdated().getEntity().getStaffSuppress());
  }

  @Test
  void testSetStaffSuppressToFalse() {
    var extendedInstance = ExtendedInstance.builder().entity(new Instance()).build();
    var actual = processor.process(IDENTIFIER, extendedInstance, rules(rule(STAFF_SUPPRESS, SET_TO_FALSE, null)));
    assertNotNull(actual.getUpdated());
    assertFalse(actual.getUpdated().getEntity().getStaffSuppress());
  }

  @Test
  void shouldNotUpdateInstanceWhenActionIsInvalid() {
    var extendedInstance = ExtendedInstance.builder().entity(new Instance().withDiscoverySuppress(true)).build();
    var actual = processor.process(IDENTIFIER, extendedInstance, rules(rule(STAFF_SUPPRESS, CLEAR_FIELD, null)));
    assertTrue(actual.getUpdated().getEntity().getDiscoverySuppress());
    verify(errorService).saveError(any(UUID.class), eq(IDENTIFIER), anyString(), eq(ErrorType.ERROR));
  }

  @Test
  void testClone() {
    var folioInstanceDataProcessor = new FolioInstanceDataProcessor(new InstanceNotesUpdaterFactory(new AdministrativeNotesUpdater(),
      new StatisticalCodesUpdater()));
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .title("Title")
      .discoverySuppress(false)
      .administrativeNotes(List.of("Note1", "Note2"))
      .build();
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();
    var cloned = folioInstanceDataProcessor.clone(extendedInstance);
    assertTrue(folioInstanceDataProcessor.compare(extendedInstance, cloned));

    cloned.getEntity().setAdministrativeNotes(Collections.singletonList("Note3"));
    assertFalse(folioInstanceDataProcessor.compare(extendedInstance, cloned));
  }

  @ParameterizedTest
  @EnumSource(value = UpdateActionType.class, names = {"MARK_AS_STAFF_ONLY", "REMOVE_MARK_AS_STAFF_ONLY"}, mode = EnumSource.Mode.INCLUDE)
  void shouldSetOrRemoveStaffOnlyForInstanceNotesByInstanceNoteTypeId(UpdateActionType actionType) {

    var typeId = UUID.randomUUID().toString();
    var staffOnly = REMOVE_MARK_AS_STAFF_ONLY.equals(actionType);
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .instanceNotes(List.of(InstanceNote.builder()
          .instanceNoteTypeId(typeId).staffOnly(staffOnly).build(),
        InstanceNote.builder()
          .instanceNoteTypeId(UUID.randomUUID().toString()).staffOnly(staffOnly).build()))
      .build();
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(INSTANCE_NOTE)
        .actions(Collections.singletonList(new Action()
          .type(actionType)
          .parameters(Collections.singletonList(new Parameter()
            .key(INSTANCE_NOTE_TYPE_ID_KEY)
            .value(typeId)))))));

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    assertTrue(result.getUpdated().getEntity().getInstanceNotes().stream()
      .filter(instanceNote -> typeId.equals(instanceNote.getInstanceNoteTypeId()))
      .map(InstanceNote::getStaffOnly)
      .allMatch(so -> so.equals(!staffOnly)));
    assertTrue(result.getUpdated().getEntity().getInstanceNotes().stream()
      .filter(instanceNote -> !typeId.equals(instanceNote.getInstanceNoteTypeId()))
      .map(InstanceNote::getStaffOnly)
      .noneMatch(so -> so.equals(!staffOnly)));
  }

  @ParameterizedTest
  @EnumSource(value = UpdateOptionType.class, names = {"ADMINISTRATIVE_NOTE", "INSTANCE_NOTE"}, mode = EnumSource.Mode.INCLUDE)
  void shouldRemoveAllAdministrativeNotesOrInstanceNotesByInstanceNoteTypeId(UpdateOptionType optionType) {
    var typeId = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .administrativeNotes(Collections.singletonList("Administrative note"))
      .instanceNotes(List.of(InstanceNote.builder()
          .instanceNoteTypeId(typeId).build(),
        InstanceNote.builder()
          .instanceNoteTypeId(UUID.randomUUID().toString()).build()))
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(optionType)
        .actions(Collections.singletonList(new Action()
          .type(REMOVE_ALL)
          .parameters(Collections.singletonList(new Parameter()
            .key(INSTANCE_NOTE_TYPE_ID_KEY)
            .value(typeId)))))));
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    if (ADMINISTRATIVE_NOTE.equals(optionType)) {
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes()).isEmpty();
      assertThat(result.getUpdated().getEntity().getInstanceNotes()).hasSize(2);
    } else {
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes()).hasSize(1);
      assertThat(result.getUpdated().getEntity().getInstanceNotes()).hasSize(1);
      assertThat(result.getUpdated().getEntity().getInstanceNotes().get(0).getInstanceNoteTypeId()).isNotEqualTo(typeId);
    }
  }

  @ParameterizedTest
  @EnumSource(value = UpdateOptionType.class, names = {"ADMINISTRATIVE_NOTE", "INSTANCE_NOTE"}, mode = EnumSource.Mode.INCLUDE)
  void shouldAddAdministrativeOrInstanceNotesByInstanceNoteTypeId(UpdateOptionType optionType) {
    var typeId = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(optionType)
        .actions(Collections.singletonList(new Action()
          .type(ADD_TO_EXISTING)
          .updated("new note")
          .parameters(Collections.singletonList(new Parameter()
            .key(INSTANCE_NOTE_TYPE_ID_KEY)
            .value(typeId)))))));
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    if (ADMINISTRATIVE_NOTE.equals(optionType)) {
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes()).hasSize(1);
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes().get(0)).isEqualTo("new note");
      assertThat(result.getUpdated().getEntity().getInstanceNotes()).isNull();
    } else {
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes()).isNull();
      assertThat(result.getUpdated().getEntity().getInstanceNotes()).hasSize(1);
      var newNote = result.getUpdated().getEntity().getInstanceNotes().get(0);
      assertThat(newNote.getNote()).isEqualTo("new note");
      assertThat(newNote.getInstanceNoteTypeId()).isEqualTo(typeId);
    }
  }

  @ParameterizedTest
  @EnumSource(value = UpdateOptionType.class, names = {"ADMINISTRATIVE_NOTE", "INSTANCE_NOTE"}, mode = EnumSource.Mode.INCLUDE)
  void shouldFindAndRemoveAdministrativeOrInstanceNotesByInstanceNoteTypeId(UpdateOptionType optionType) {
    var typeId = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .administrativeNotes(List.of("first note", "First note"))
      .instanceNotes(List.of(InstanceNote.builder()
          .instanceNoteTypeId(typeId).note("first note").build(),
        InstanceNote.builder()
          .instanceNoteTypeId(typeId).note("First note").build()))
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(optionType)
        .actions(Collections.singletonList(new Action()
          .type(FIND_AND_REMOVE_THESE)
          .initial("first")
          .parameters(Collections.singletonList(new Parameter()
            .key(INSTANCE_NOTE_TYPE_ID_KEY)
            .value(typeId)))))));

    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    if (ADMINISTRATIVE_NOTE.equals(optionType)) {
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes()).hasSize(2);
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes().get(0)).isEqualTo(" note");
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes().get(1)).isEqualTo("First note");
      assertThat(result.getUpdated().getEntity().getInstanceNotes()).hasSize(2);
    } else {
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes()).hasSize(2);
      assertThat(result.getUpdated().getEntity().getInstanceNotes()).hasSize(2);
      assertThat(result.getUpdated().getEntity().getInstanceNotes().get(0).getNote()).isEqualTo(" note");
      assertThat(result.getUpdated().getEntity().getInstanceNotes().get(1).getNote()).isEqualTo("First note");
    }
  }

  @ParameterizedTest
  @EnumSource(value = UpdateOptionType.class, names = {"ADMINISTRATIVE_NOTE", "INSTANCE_NOTE"}, mode = EnumSource.Mode.INCLUDE)
  void shouldFindAndReplaceAdministrativeOrInstanceNotesByInstanceNoteTypeId(UpdateOptionType optionType) {
    var typeId = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .administrativeNotes(List.of("first note", "First note"))
      .instanceNotes(List.of(InstanceNote.builder()
          .instanceNoteTypeId(typeId).note("first note").build(),
        InstanceNote.builder()
          .instanceNoteTypeId(typeId).note("First note").build()))
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(optionType)
        .actions(Collections.singletonList(new Action()
          .type(FIND_AND_REPLACE)
          .initial("first")
          .updated("updated first")
          .parameters(Collections.singletonList(new Parameter()
            .key(INSTANCE_NOTE_TYPE_ID_KEY)
            .value(typeId)))))));

    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    if (ADMINISTRATIVE_NOTE.equals(optionType)) {
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes()).hasSize(2);
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes().get(0)).isEqualTo("updated first note");
      assertThat(result.getUpdated().getEntity().getInstanceNotes()).hasSize(2);
    } else {
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes()).hasSize(2);
      assertThat(result.getUpdated().getEntity().getInstanceNotes()).hasSize(2);
      assertThat(result.getUpdated().getEntity().getInstanceNotes().get(0).getNote()).isEqualTo("updated first note");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"5d47c42c-86ef-4390-a006-9c29cf7529be", "ADMINISTRATIVE_NOTE"})
  void shouldChangeNoteTypeForInstanceNotesByInstanceNoteTypeId(String newType) {
    var typeId = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .instanceNotes(List.of(InstanceNote.builder()
          .instanceNoteTypeId(typeId).note("first note").build(),
        InstanceNote.builder()
          .instanceNoteTypeId(UUID.randomUUID().toString()).note("second note").build()))
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(INSTANCE_NOTE)
        .actions(Collections.singletonList(new Action()
          .type(CHANGE_TYPE)
          .updated(newType)
          .parameters(Collections.singletonList(new Parameter()
            .key(INSTANCE_NOTE_TYPE_ID_KEY)
            .value(typeId)))))));

    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    if (ADMINISTRATIVE_NOTE.getValue().equals(newType)) {
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes()).hasSize(1);
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes().get(0)).isEqualTo("first note");
      assertThat(result.getUpdated().getEntity().getInstanceNotes()).hasSize(1);
      assertThat(result.getUpdated().getEntity().getInstanceNotes().get(0).getNote()).isEqualTo("second note");
    } else {
      assertThat(result.getUpdated().getEntity().getAdministrativeNotes()).isNull();
      assertThat(result.getUpdated().getEntity().getInstanceNotes()).hasSize(2);
      var updatedNote = result.getUpdated().getEntity().getInstanceNotes().get(0);
      assertThat(updatedNote.getNote()).isEqualTo("first note");
      assertThat(updatedNote.getInstanceNoteTypeId()).isEqualTo(newType);
    }
  }

  @Test
  void shouldChangeNoteTypeForAdministrativeNotes() {
    var typeId = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .administrativeNotes(List.of("first note", "Second note"))
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(ADMINISTRATIVE_NOTE)
        .actions(Collections.singletonList(new Action()
          .type(CHANGE_TYPE)
          .updated(typeId)))));

    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();
    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    assertThat(result.getUpdated().getEntity().getAdministrativeNotes()).isEmpty();
    assertThat(result.getUpdated().getEntity().getInstanceNotes()).hasSize(2);
    assertThat(result.getUpdated().getEntity().getInstanceNotes()).allMatch(note -> typeId.equals(note.getInstanceNoteTypeId()));
  }

  @ParameterizedTest
  @EnumSource(value = UpdateActionType.class,
    names = {"MARK_AS_STAFF_ONLY",
      "REMOVE_MARK_AS_STAFF_ONLY",
      "REMOVE_ALL",
      "ADD_TO_EXISTING",
      "FIND_AND_REMOVE_THESE",
      "FIND_AND_REPLACE",
      "CHANGE_TYPE"},
    mode = EnumSource.Mode.INCLUDE)
  void shouldNotUpdateNotesIfSourceIsNotFolio(UpdateActionType actionType) {
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("MARC")
      .title("Sample title")
      .build();

    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();
    var validator = ((FolioInstanceDataProcessor) processor).validator(extendedInstance);

    assertThrows(RuleValidationException.class, () -> validator.validate(INSTANCE_NOTE, new Action().type(actionType), new BulkOperationRule()));
  }

  @Test
  void shouldNotChangeTypeForAdministrativeNotesIfSourceIsNotFolio() {
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("MARC")
      .title("Sample title")
      .build();
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();
    var validator = ((FolioInstanceDataProcessor) processor).validator(extendedInstance);

    assertThrows(RuleValidationException.class, () -> validator.validate(ADMINISTRATIVE_NOTE, new Action().type(CHANGE_TYPE), new BulkOperationRule()));
  }

  @Test
  void shouldThrowExceptionIfStatisticalCodeRemoveAllCombinedWithOtherActions() {
    var validator = ((FolioInstanceDataProcessor) processor).validator();
    var rules = new BulkOperationRuleCollection();
    rules.addBulkOperationRulesItem(new BulkOperationRule().ruleDetails(
      new RuleDetails().actions(List.of(new Action().type(ADD_TO_EXISTING))).option(UpdateOptionType.STATISTICAL_CODE)));
    rules.addBulkOperationRulesItem(new BulkOperationRule().ruleDetails(
      new RuleDetails().actions(List.of(new Action().type(REMOVE_ALL))).option(UpdateOptionType.STATISTICAL_CODE)));

    assertThrows(RuleValidationException.class, () -> validator.validate(rules));
  }

  @Test
  void shouldAddToExistingMoreThanOneStatisticalCodes() {
    var statCode1 = UUID.randomUUID().toString();
    var statCode2 = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(UpdateOptionType.STATISTICAL_CODE)
        .actions(Collections.singletonList(new Action()
          .type(ADD_TO_EXISTING).updated(statCode1 + "," + statCode2)))));
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    assertEquals("[" + statCode1 + ", " + statCode2 + "]", result.getUpdated().getEntity().getStatisticalCodeIds().toString());
    assertEquals("[" + statCode1 + ", " + statCode2 + "]", result.getPreview().getEntity().getStatisticalCodeIds().toString());
  }

  @Test
  void shouldAddToExistingIfAlreadyPresentStatisticalCodes() {
    var statCode1 = UUID.randomUUID().toString();
    var statCode2 = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .statisticalCodeIds(List.of(statCode1))
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(UpdateOptionType.STATISTICAL_CODE)
        .actions(Collections.singletonList(new Action()
          .type(ADD_TO_EXISTING).updated(statCode1 + "," + statCode2)))));
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    assertEquals("[" + statCode1 + ", " + statCode1 + ", " + statCode2 + "]",
      result.getPreview().getEntity().getStatisticalCodeIds().toString());
    assertEquals("[" + statCode1 + ", " + statCode2 + "]",
      result.getUpdated().getEntity().getStatisticalCodeIds().toString());
  }

  @Test
  void shouldAddDuplicatesToExistingIfNotPresentStatisticalCodes() {
    var statCode1 = UUID.randomUUID().toString();
    var statCode2 = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(UpdateOptionType.STATISTICAL_CODE)
        .actions(Collections.singletonList(new Action()
          .type(ADD_TO_EXISTING).updated(statCode1 + "," + statCode2 + "," + statCode1)))));
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    assertEquals("[" + statCode1 + ", " + statCode2 + ", " + statCode1 + "]",
      result.getPreview().getEntity().getStatisticalCodeIds().toString());
    assertEquals("[" + statCode1 + ", " + statCode2 + "]",
      result.getUpdated().getEntity().getStatisticalCodeIds().toString());
  }

  @Test
  void shouldAddDuplicatesToExistingIfPresentStatisticalCodes() {
    var statCode1 = UUID.randomUUID().toString();
    var statCode2 = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .statisticalCodeIds(List.of(statCode2))
      .title("Sample title")
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(UpdateOptionType.STATISTICAL_CODE)
        .actions(Collections.singletonList(new Action()
          .type(ADD_TO_EXISTING).updated(statCode1 + "," + statCode2 + "," + statCode1)))));
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    assertEquals("[" + statCode2 + ", " + statCode1 + ", " + statCode2 + ", " + statCode1 + "]",
      result.getPreview().getEntity().getStatisticalCodeIds().toString());
    assertEquals("[" + statCode2 + ", " + statCode1 + "]",
      result.getUpdated().getEntity().getStatisticalCodeIds().toString());
  }

  @Test
  void shouldRemoveMoreThanOneStatisticalCodes() {
    var statCode1 = UUID.randomUUID().toString();
    var statCode2 = UUID.randomUUID().toString();
    var statCode3 = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .statisticalCodeIds(List.of(statCode1, statCode2, statCode3))
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(UpdateOptionType.STATISTICAL_CODE)
        .actions(Collections.singletonList(new Action()
          .type(REMOVE_SOME).updated(statCode1 + "," + statCode2)))));
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    assertThat(result.getUpdated().getEntity().getStatisticalCodeIds()).hasSize(1);
    assertThat(result.getPreview().getEntity().getStatisticalCodeIds()).hasSize(1);
    assertEquals(statCode3, result.getUpdated().getEntity().getStatisticalCodeIds().get(0));
  }

  @Test
  void shouldRemoveNothingIfNoStatisticalCodes() {
    var statCode1 = UUID.randomUUID().toString();
    var statCode2 = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .title("Sample title")
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(UpdateOptionType.STATISTICAL_CODE)
        .actions(Collections.singletonList(new Action()
          .type(REMOVE_SOME).updated(statCode1 + "," + statCode2)))));
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    assertThat(result.getUpdated().getEntity().getStatisticalCodeIds()).isEmpty();
    assertThat(result.getPreview().getEntity().getStatisticalCodeIds()).isEmpty();
  }

  @Test
  void shouldRemoveAllStatisticalCodes() {
    var statCode1 = UUID.randomUUID().toString();
    var statCode2 = UUID.randomUUID().toString();
    var instance = Instance.builder()
      .id(UUID.randomUUID().toString())
      .source("FOLIO")
      .statisticalCodeIds(List.of(statCode1, statCode2))
      .title("Sample title")
      .build();

    var rules = rules(new BulkOperationRule()
      .ruleDetails(new RuleDetails()
        .option(UpdateOptionType.STATISTICAL_CODE)
        .actions(Collections.singletonList(new Action()
          .type(REMOVE_ALL)))));
    var extendedInstance = ExtendedInstance.builder().entity(instance).tenantId("tenantId").build();

    var result = processor.process(IDENTIFIER, extendedInstance, rules);

    assertThat(result.getUpdated().getEntity().getStatisticalCodeIds()).isEmpty();
    assertThat(result.getPreview().getEntity().getStatisticalCodeIds()).isEmpty();
  }
}
