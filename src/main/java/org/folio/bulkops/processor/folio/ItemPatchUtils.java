package org.folio.bulkops.processor.folio;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.bean.Item.ITEM_JSON_ADM_NOTES;
import static org.folio.bulkops.domain.bean.Item.ITEM_JSON_CIRCULATION_NOTES;
import static org.folio.bulkops.domain.bean.Item.ITEM_JSON_DISCOVERY_SUPPRESS;
import static org.folio.bulkops.domain.bean.Item.ITEM_JSON_ID;
import static org.folio.bulkops.domain.bean.Item.ITEM_JSON_NOTES;
import static org.folio.bulkops.domain.bean.Item.ITEM_JSON_PERMANENT_LOAN_TYPE;
import static org.folio.bulkops.domain.bean.Item.ITEM_JSON_STATUS;
import static org.folio.bulkops.domain.bean.Item.ITEM_JSON_TEMPORARY_LOAN_TYPE;
import static org.folio.bulkops.domain.bean.Item.ITEM_JSON_VERSION;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;

import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.ObjectNode;

@UtilityClass
public class ItemPatchUtils {
  private static final ObjectMapper mapper = new ObjectMapper();

  public static ObjectNode fetchChangedData(Item item, BulkOperationRuleCollection rules) {
    var result = createPatchBody(item);

    var details =
        rules.getBulkOperationRules().stream()
            .map(BulkOperationRule::getRuleDetails)
            .filter(Objects::nonNull)
            .toList();

    for (var ruleDetails : details) {
      if (ruleDetails.getOption() != null) {
        switch (ruleDetails.getOption()) {
          case SUPPRESS_FROM_DISCOVERY -> addDiscoverySuppress(result, item.getDiscoverySuppress());
          case CHECK_IN_NOTE, CHECK_OUT_NOTE -> addCirculationNotes(result, item);
          case STATUS -> addStatus(result, item);
          case PERMANENT_LOAN_TYPE -> addPermanentLoanType(result, item);
          case TEMPORARY_LOAN_TYPE -> addTemporaryLoanType(result, item);
          case PERMANENT_LOCATION -> addPermanentLocation(result, item);
          case TEMPORARY_LOCATION -> addTemporaryLocation(result, item);
          case ADMINISTRATIVE_NOTE -> {
            addAdministrativeNotes(result, item);
            if (CHANGE_TYPE.equals(ruleDetails.getActions().getFirst().getType())) {
              addItemNotes(result, item);
            }
          }
          case ITEM_NOTE -> {
            addItemNotes(result, item);
            if (CHANGE_TYPE.equals(ruleDetails.getActions().getFirst().getType())
                && ADMINISTRATIVE_NOTE
                    .getValue()
                    .equals(ruleDetails.getActions().getFirst().getUpdated())) {
              addAdministrativeNotes(result, item);
            }
          }
          default ->
              throw new IllegalArgumentException(
                  "Rule option %s is not supported".formatted(ruleDetails.getOption()));
        }
      }
    }
    return result;
  }

  public static ObjectNode createPatchBody(Item item) {
    var result = mapper.createObjectNode();
    result.put(ITEM_JSON_ID, item.getId());
    result.set(
        ITEM_JSON_VERSION,
        isNull(item.getVersion()) ? mapper.nullNode() : IntNode.valueOf(item.getVersion()));
    return result;
  }

  public static void addDiscoverySuppress(ObjectNode node, Boolean value) {
    node.set(
        ITEM_JSON_DISCOVERY_SUPPRESS,
        isNull(value) ? mapper.nullNode() : BooleanNode.valueOf(value));
  }

  private static void addAdministrativeNotes(ObjectNode node, Item item) {
    node.set(ITEM_JSON_ADM_NOTES, mapper.valueToTree(item.getAdministrativeNotes()));
  }

  private static void addItemNotes(ObjectNode node, Item item) {
    node.set(ITEM_JSON_NOTES, mapper.valueToTree(item.getNotes()));
  }

  private static void addCirculationNotes(ObjectNode node, Item item) {
    node.set(ITEM_JSON_CIRCULATION_NOTES, mapper.valueToTree(item.getCirculationNotes()));
  }

  private static void addStatus(ObjectNode node, Item item) {
    node.set(ITEM_JSON_STATUS, mapper.valueToTree(item.getStatus()));
  }

  private static void addPermanentLoanType(ObjectNode node, Item item) {
    node.set(ITEM_JSON_PERMANENT_LOAN_TYPE, mapper.valueToTree(item.getPermanentLoanType()));
  }

  private static void addTemporaryLoanType(ObjectNode node, Item item) {
    node.set(ITEM_JSON_TEMPORARY_LOAN_TYPE, mapper.valueToTree(item.getTemporaryLoanType()));
  }

  private static void addPermanentLocation(ObjectNode node, Item item) {
    node.set(ITEM_JSON_PERMANENT_LOAN_TYPE, mapper.valueToTree(item.getPermanentLocation()));
  }

  private static void addTemporaryLocation(ObjectNode node, Item item) {
    node.set(ITEM_JSON_TEMPORARY_LOAN_TYPE, mapper.valueToTree(item.getTemporaryLocation()));
  }
}
