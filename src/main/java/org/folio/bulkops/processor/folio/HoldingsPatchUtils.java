package org.folio.bulkops.processor.folio;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_ADMINISTRATIVE_NOTES;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_DISCOVERY_SUPPRESS;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_ELECTRONIC_ACCESS;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_HOLDINGS_NOTES;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_ID;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_PERMANENT_LOCATION_ID;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_TEMPORARY_LOCATION_ID;
import static org.folio.bulkops.domain.bean.HoldingsRecord.HOLDINGS_JSON_VERSION;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;

import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.ObjectNode;

@UtilityClass
public class HoldingsPatchUtils {
  private static final ObjectMapper mapper = new ObjectMapper();

  public static ObjectNode fetchChangedData(
      HoldingsRecord holdingsRecord, BulkOperationRuleCollection rules) {
    var result = createEmptyPatch(holdingsRecord);

    var details =
        rules.getBulkOperationRules().stream()
            .map(BulkOperationRule::getRuleDetails)
            .filter(Objects::nonNull)
            .toList();

    for (var ruleDetails : details) {
      if (ruleDetails.getOption() != null) {
        switch (ruleDetails.getOption()) {
          case ADMINISTRATIVE_NOTE -> {
            addAdministrativeNotes(result, holdingsRecord);
            if (CHANGE_TYPE.equals(ruleDetails.getActions().getFirst().getType())) {
              addHoldingsNotes(result, holdingsRecord);
            }
          }
          case ELECTRONIC_ACCESS_LINK_TEXT,
              ELECTRONIC_ACCESS_MATERIALS_SPECIFIED,
              ELECTRONIC_ACCESS_URI,
              ELECTRONIC_ACCESS_URL_PUBLIC_NOTE,
              ELECTRONIC_ACCESS_URL_RELATIONSHIP ->
              addElectronicAccess(result, holdingsRecord);
          case HOLDINGS_NOTE -> {
            addHoldingsNotes(result, holdingsRecord);
            if (CHANGE_TYPE.equals(ruleDetails.getActions().getFirst().getType())
                && ADMINISTRATIVE_NOTE
                    .getValue()
                    .equals(ruleDetails.getActions().getFirst().getUpdated())) {
              addAdministrativeNotes(result, holdingsRecord);
            }
          }
          case PERMANENT_LOCATION -> addPermanentLocation(result, holdingsRecord);
          case SUPPRESS_FROM_DISCOVERY -> addDiscoverySuppress(result, holdingsRecord);
          case TEMPORARY_LOCATION -> addTemporaryLocation(result, holdingsRecord);
          default ->
              throw new IllegalArgumentException(
                  "Rule option %s is not supported".formatted(ruleDetails.getOption()));
        }
      }
    }
    return result;
  }

  public static ObjectNode createPatchForSuppress(HoldingsRecord holdingsRecord, boolean suppress) {
    var result = createEmptyPatch(holdingsRecord);
    result.put(HOLDINGS_JSON_DISCOVERY_SUPPRESS, suppress);
    return result;
  }

  private static ObjectNode createEmptyPatch(HoldingsRecord holdingsRecord) {
    var result = mapper.createObjectNode();
    result.put(HOLDINGS_JSON_ID, holdingsRecord.getId());
    result.set(
        HOLDINGS_JSON_VERSION,
        isNull(holdingsRecord.getVersion())
            ? mapper.nullNode()
            : IntNode.valueOf(holdingsRecord.getVersion()));
    return result;
  }

  private static void addDiscoverySuppress(ObjectNode node, HoldingsRecord holdingsRecord) {
    node.set(
        HOLDINGS_JSON_DISCOVERY_SUPPRESS,
        isNull(holdingsRecord.getDiscoverySuppress())
            ? mapper.nullNode()
            : BooleanNode.valueOf(holdingsRecord.getDiscoverySuppress()));
  }

  private static void addAdministrativeNotes(ObjectNode node, HoldingsRecord holdingsRecord) {
    node.set(
        HOLDINGS_JSON_ADMINISTRATIVE_NOTES,
        mapper.valueToTree(holdingsRecord.getAdministrativeNotes()));
  }

  private static void addHoldingsNotes(ObjectNode node, HoldingsRecord holdingsRecord) {
    node.set(HOLDINGS_JSON_HOLDINGS_NOTES, mapper.valueToTree(holdingsRecord.getNotes()));
  }

  private static void addPermanentLocation(ObjectNode node, HoldingsRecord holdingsRecord) {
    node.set(
        HOLDINGS_JSON_PERMANENT_LOCATION_ID,
        mapper.valueToTree(holdingsRecord.getPermanentLocationId()));
  }

  private static void addTemporaryLocation(ObjectNode node, HoldingsRecord holdingsRecord) {
    node.set(
        HOLDINGS_JSON_TEMPORARY_LOCATION_ID,
        mapper.valueToTree(holdingsRecord.getTemporaryLocationId()));
  }

  private static void addElectronicAccess(ObjectNode node, HoldingsRecord holdingsRecord) {
    node.set(
        HOLDINGS_JSON_ELECTRONIC_ACCESS, mapper.valueToTree(holdingsRecord.getElectronicAccess()));
  }
}
