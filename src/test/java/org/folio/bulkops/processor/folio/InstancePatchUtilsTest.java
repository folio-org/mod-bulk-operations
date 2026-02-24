package org.folio.bulkops.processor.folio;

import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_ADM_NOTES;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_DELETED;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_DISCOVERY_SUPPRESS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_ID;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_NOTES;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_STAFF_SUPPRESS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_STATISTICAL_CODES;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.InstanceNote;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.RuleDetails;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.junit.jupiter.api.Test;

class InstancePatchUtilsTest {

  @Test
  void shouldPopulateStaffSuppress() {
    var id = UUID.randomUUID().toString();
    var version = 1;
    var instance = Instance.builder().id(id).version(version).staffSuppress(true).build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.STAFF_SUPPRESS, action(UpdateActionType.REPLACE_WITH)));

    var changed = InstancePatchUtils.fetchChangedData(instance, rules);

    assertTrue(changed.has(INSTANCE_JSON_STAFF_SUPPRESS));
    assertTrue(changed.get(INSTANCE_JSON_STAFF_SUPPRESS).asBoolean());
    assertEquals(id, changed.get(INSTANCE_JSON_ID).asString());
    assertEquals(version, changed.get(INSTANCE_JSON_VERSION).asInt());
    assertEquals(3, changed.size());
  }

  @Test
  void shouldPopulateSuppressFromDiscovery() {
    var instance = Instance.builder().discoverySuppress(false).build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.SUPPRESS_FROM_DISCOVERY, action(UpdateActionType.REPLACE_WITH)));

    var changed = InstancePatchUtils.fetchChangedData(instance, rules);

    assertTrue(changed.has(INSTANCE_JSON_DISCOVERY_SUPPRESS));
    assertFalse(changed.get(INSTANCE_JSON_DISCOVERY_SUPPRESS).asBoolean());
    assertEquals(3, changed.size());
  }

  @Test
  void shouldPopulateSetRecordsForDelete() {
    var instance =
        Instance.builder().staffSuppress(true).discoverySuppress(false).deleted(true).build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.SET_RECORDS_FOR_DELETE, action(UpdateActionType.REPLACE_WITH)));

    var changed = InstancePatchUtils.fetchChangedData(instance, rules);

    assertTrue(changed.get(INSTANCE_JSON_STAFF_SUPPRESS).asBoolean());
    assertFalse(changed.get(INSTANCE_JSON_DISCOVERY_SUPPRESS).asBoolean());
    assertTrue(changed.get(INSTANCE_JSON_DELETED).asBoolean());
    assertEquals(5, changed.size());
  }

  @Test
  void shouldPopulateStatisticalCodes() {
    var instance = Instance.builder().statisticalCodeIds(List.of("sc1", "sc2")).build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.STATISTICAL_CODE, action(UpdateActionType.ADD_TO_EXISTING)));

    var changed = InstancePatchUtils.fetchChangedData(instance, rules);

    assertTrue(changed.has(INSTANCE_JSON_STATISTICAL_CODES));
    assertEquals(
        List.of("sc1", "sc2"),
        List.of(
            changed.get(INSTANCE_JSON_STATISTICAL_CODES).get(0).asString(),
            changed.get(INSTANCE_JSON_STATISTICAL_CODES).get(1).asString()));
    assertEquals(3, changed.size());
  }

  @Test
  void shouldPopulateAdministrativeNotesOnly_whenNotChangeType() {
    var instance =
        Instance.builder()
            .administrativeNotes(List.of("a1"))
            .instanceNotes(List.of(note("n1")))
            .build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.ADMINISTRATIVE_NOTE, action(UpdateActionType.REPLACE_WITH)));

    var changed = InstancePatchUtils.fetchChangedData(instance, rules);

    assertTrue(changed.has(INSTANCE_JSON_ADM_NOTES));
    assertFalse(changed.has(INSTANCE_JSON_NOTES));
    assertEquals(3, changed.size());
  }

  @Test
  void shouldPopulateAdministrativeNotesAndNotes_whenChangeType() {
    var instance =
        Instance.builder()
            .administrativeNotes(List.of("a1"))
            .instanceNotes(List.of(note("n1")))
            .build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.ADMINISTRATIVE_NOTE, action(UpdateActionType.CHANGE_TYPE)));

    var changed = InstancePatchUtils.fetchChangedData(instance, rules);

    assertTrue(changed.has(INSTANCE_JSON_ADM_NOTES));
    assertTrue(changed.has(INSTANCE_JSON_NOTES));
    assertEquals(4, changed.size());
  }

  @Test
  void shouldPopulateInstanceNotesOnly_whenNotChangeType() {
    var instance =
        Instance.builder()
            .administrativeNotes(List.of("a1"))
            .instanceNotes(List.of(note("n1")))
            .build();
    var rules =
        ruleCollection(rule(UpdateOptionType.INSTANCE_NOTE, action(UpdateActionType.REPLACE_WITH)));

    var changed = InstancePatchUtils.fetchChangedData(instance, rules);

    assertTrue(changed.has(INSTANCE_JSON_NOTES));
    assertFalse(changed.has(INSTANCE_JSON_ADM_NOTES));
    assertEquals(3, changed.size());
  }

  @Test
  void shouldPopulateInstanceNotesAndAdministrativeNotes_whenChangeTypeToAdministrativeNote() {
    var instance =
        Instance.builder()
            .administrativeNotes(List.of("a1"))
            .instanceNotes(List.of(note("n1")))
            .build();
    var rules =
        ruleCollection(
            rule(
                UpdateOptionType.INSTANCE_NOTE,
                action(UpdateActionType.CHANGE_TYPE)
                    .updated(UpdateOptionType.ADMINISTRATIVE_NOTE.getValue())));

    var changed = InstancePatchUtils.fetchChangedData(instance, rules);

    assertTrue(changed.has(INSTANCE_JSON_NOTES));
    assertTrue(changed.has(INSTANCE_JSON_ADM_NOTES));
    assertEquals(4, changed.size());
  }

  @Test
  void shouldIgnoreRuleWithNullDetails() {
    var instance = Instance.builder().staffSuppress(true).build();
    var rules =
        new BulkOperationRuleCollection().bulkOperationRules(List.of(new BulkOperationRule()));

    var changed = InstancePatchUtils.fetchChangedData(instance, rules);

    assertEquals(2, changed.size());
  }

  @Test
  void shouldIgnoreRuleWithNullOption() {
    var instance = Instance.builder().staffSuppress(true).build();
    var rules =
        new BulkOperationRuleCollection()
            .bulkOperationRules(
                List.of(
                    new BulkOperationRule()
                        .ruleDetails(
                            new RuleDetails()
                                .actions(List.of(action(UpdateActionType.REPLACE_WITH))))));

    var changed = InstancePatchUtils.fetchChangedData(instance, rules);

    assertEquals(2, changed.size());
  }

  @Test
  void shouldThrowForUnsupportedOption() {
    var instance = Instance.builder().staffSuppress(true).build();
    var rules =
        ruleCollection(
            rule(
                UpdateOptionType.PERMANENT_LOCATION,
                action(UpdateActionType.REPLACE_WITH))); // not supported by InstancePatchUtils

    assertThrows(
        IllegalArgumentException.class, () -> InstancePatchUtils.fetchChangedData(instance, rules));
  }

  private static BulkOperationRuleCollection ruleCollection(BulkOperationRule... rules) {
    return new BulkOperationRuleCollection().bulkOperationRules(List.of(rules));
  }

  private static BulkOperationRule rule(UpdateOptionType option, Action action) {
    return new BulkOperationRule()
        .ruleDetails(new RuleDetails().option(option).actions(List.of(action)));
  }

  private static Action action(UpdateActionType type) {
    return new Action().type(type);
  }

  private static InstanceNote note(String note) {
    return new InstanceNote().withNote(note);
  }
}
