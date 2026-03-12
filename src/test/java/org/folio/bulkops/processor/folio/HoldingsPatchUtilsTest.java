package org.folio.bulkops.processor.folio;

import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_ADMINISTRATIVE_NOTES;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_DISCOVERY_SUPPRESS;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_ELECTRONIC_ACCESS;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_HOLDINGS_NOTES;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_ID;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_PERMANENT_LOCATION_ID;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_TEMPORARY_LOCATION_ID;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.folio.bulkops.domain.bean.ElectronicAccess;
import org.folio.bulkops.domain.bean.HoldingsNote;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.RuleDetails;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.junit.jupiter.api.Test;

class HoldingsPatchUtilsTest {

  @Test
  void shouldPopulateSuppressFromDiscovery() {
    var id = UUID.randomUUID().toString();
    var version = 1;
    var holdings =
        HoldingsRecord.builder().id(id).version(version).discoverySuppress(false).build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.SUPPRESS_FROM_DISCOVERY, action(UpdateActionType.REPLACE_WITH)));

    var changed = HoldingsPatchUtils.fetchChangedData(holdings, rules);

    assertTrue(changed.has(HOLDINGS_JSON_DISCOVERY_SUPPRESS));
    assertFalse(changed.get(HOLDINGS_JSON_DISCOVERY_SUPPRESS).asBoolean());
    assertEquals(id, changed.get(HOLDINGS_JSON_ID).asString());
    assertEquals(version, changed.get(HOLDINGS_JSON_VERSION).asInt());
    assertEquals(3, changed.size());
  }

  @Test
  void shouldPopulateVersionAsNullNode_whenVersionIsNull() {
    var holdings = HoldingsRecord.builder().id(UUID.randomUUID().toString()).version(null).build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.SUPPRESS_FROM_DISCOVERY, action(UpdateActionType.REPLACE_WITH)));

    var changed = HoldingsPatchUtils.fetchChangedData(holdings, rules);

    assertTrue(changed.has(HOLDINGS_JSON_VERSION));
    assertTrue(changed.get(HOLDINGS_JSON_VERSION).isNull());
  }

  @Test
  void shouldPopulateAdministrativeNotesOnly_whenNotChangeType() {
    var holdings =
        HoldingsRecord.builder()
            .administrativeNotes(List.of("a1"))
            .notes(List.of(note("n1")))
            .build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.ADMINISTRATIVE_NOTE, action(UpdateActionType.REPLACE_WITH)));

    var changed = HoldingsPatchUtils.fetchChangedData(holdings, rules);

    assertTrue(changed.has(HOLDINGS_JSON_ADMINISTRATIVE_NOTES));
    assertFalse(changed.has(HOLDINGS_JSON_HOLDINGS_NOTES));
    assertEquals(3, changed.size());
  }

  @Test
  void shouldPopulateAdministrativeNotesAndHoldingsNotes_whenChangeType() {
    var holdings =
        HoldingsRecord.builder()
            .administrativeNotes(List.of("a1"))
            .notes(List.of(note("n1")))
            .build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.ADMINISTRATIVE_NOTE, action(UpdateActionType.CHANGE_TYPE)));

    var changed = HoldingsPatchUtils.fetchChangedData(holdings, rules);

    assertTrue(changed.has(HOLDINGS_JSON_ADMINISTRATIVE_NOTES));
    assertTrue(changed.has(HOLDINGS_JSON_HOLDINGS_NOTES));
    assertEquals(4, changed.size());
  }

  @Test
  void shouldPopulateHoldingsNotesOnly_whenNotChangeType() {
    var holdings =
        HoldingsRecord.builder()
            .administrativeNotes(List.of("a1"))
            .notes(List.of(note("n1")))
            .build();
    var rules =
        ruleCollection(rule(UpdateOptionType.HOLDINGS_NOTE, action(UpdateActionType.REPLACE_WITH)));

    var changed = HoldingsPatchUtils.fetchChangedData(holdings, rules);

    assertTrue(changed.has(HOLDINGS_JSON_HOLDINGS_NOTES));
    assertFalse(changed.has(HOLDINGS_JSON_ADMINISTRATIVE_NOTES));
    assertEquals(3, changed.size());
  }

  @Test
  void shouldPopulateHoldingsNotesAndAdministrativeNotes_whenChangeTypeToAdministrativeNote() {
    var holdings =
        HoldingsRecord.builder()
            .administrativeNotes(List.of("a1"))
            .notes(List.of(note("n1")))
            .build();
    var rules =
        ruleCollection(
            rule(
                UpdateOptionType.HOLDINGS_NOTE,
                action(UpdateActionType.CHANGE_TYPE)
                    .updated(UpdateOptionType.ADMINISTRATIVE_NOTE.getValue())));

    var changed = HoldingsPatchUtils.fetchChangedData(holdings, rules);

    assertTrue(changed.has(HOLDINGS_JSON_HOLDINGS_NOTES));
    assertTrue(changed.has(HOLDINGS_JSON_ADMINISTRATIVE_NOTES));
    assertEquals(4, changed.size());
  }

  @Test
  void shouldPopulatePermanentLocation() {
    var holdings = HoldingsRecord.builder().permanentLocationId("loc1").build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.PERMANENT_LOCATION, action(UpdateActionType.REPLACE_WITH)));

    var changed = HoldingsPatchUtils.fetchChangedData(holdings, rules);

    assertTrue(changed.has(HOLDINGS_JSON_PERMANENT_LOCATION_ID));
    assertEquals("loc1", changed.get(HOLDINGS_JSON_PERMANENT_LOCATION_ID).asString());
    assertEquals(3, changed.size());
  }

  @Test
  void shouldPopulateTemporaryLocation() {
    var holdings = HoldingsRecord.builder().temporaryLocationId("loc2").build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.TEMPORARY_LOCATION, action(UpdateActionType.REPLACE_WITH)));

    var changed = HoldingsPatchUtils.fetchChangedData(holdings, rules);

    assertTrue(changed.has(HOLDINGS_JSON_TEMPORARY_LOCATION_ID));
    assertEquals("loc2", changed.get(HOLDINGS_JSON_TEMPORARY_LOCATION_ID).asString());
    assertEquals(3, changed.size());
  }

  @Test
  void shouldPopulateElectronicAccess_forAnyElectronicAccessOption() {
    var holdings =
        HoldingsRecord.builder()
            .electronicAccess(List.of(new ElectronicAccess().withUri("https://example.org")))
            .build();

    var rules =
        ruleCollection(
            rule(UpdateOptionType.ELECTRONIC_ACCESS_URI, action(UpdateActionType.REPLACE_WITH)));

    var changed = HoldingsPatchUtils.fetchChangedData(holdings, rules);

    assertTrue(changed.has(HOLDINGS_JSON_ELECTRONIC_ACCESS));
    assertEquals(
        "https://example.org",
        changed.get(HOLDINGS_JSON_ELECTRONIC_ACCESS).get(0).get("uri").asString());
    assertEquals(3, changed.size());
  }

  @Test
  void shouldIgnoreRuleWithNullDetails() {
    var holdings = HoldingsRecord.builder().discoverySuppress(true).build();
    var rules =
        new BulkOperationRuleCollection().bulkOperationRules(List.of(new BulkOperationRule()));

    var changed = HoldingsPatchUtils.fetchChangedData(holdings, rules);

    assertEquals(2, changed.size());
  }

  @Test
  void shouldIgnoreRuleWithNullOption() {
    var holdings = HoldingsRecord.builder().discoverySuppress(true).build();
    var rules =
        new BulkOperationRuleCollection()
            .bulkOperationRules(
                List.of(
                    new BulkOperationRule()
                        .ruleDetails(
                            new RuleDetails()
                                .actions(List.of(action(UpdateActionType.REPLACE_WITH))))));

    var changed = HoldingsPatchUtils.fetchChangedData(holdings, rules);

    assertEquals(2, changed.size());
  }

  @Test
  void shouldThrowForUnsupportedOption() {
    var holdings = HoldingsRecord.builder().discoverySuppress(true).build();
    var rules =
        ruleCollection(
            rule(UpdateOptionType.STAFF_SUPPRESS, action(UpdateActionType.REPLACE_WITH)));

    assertThrows(
        IllegalArgumentException.class, () -> HoldingsPatchUtils.fetchChangedData(holdings, rules));
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

  private static HoldingsNote note(String note) {
    return new HoldingsNote().withNote(note);
  }
}
