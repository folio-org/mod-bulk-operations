package org.folio.bulkops.processor.folio;

import static java.util.Objects.isNull;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_ADM_NOTES;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_DELETED;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_DISCOVERY_SUPPRESS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_ID;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_NOTES;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_STAFF_SUPPRESS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_STATISTICAL_CODES;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_JSON_VERSION;
import static org.folio.bulkops.domain.dto.UpdateActionType.CHANGE_TYPE;
import static org.folio.bulkops.domain.dto.UpdateOptionType.ADMINISTRATIVE_NOTE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;

@UtilityClass
public class InstancePatchUtils {
  private static final ObjectMapper mapper = new ObjectMapper();

  public static ObjectNode fetchChangedData(Instance instance, BulkOperationRuleCollection rules) {
    var result = mapper.createObjectNode();

    result.put(INSTANCE_JSON_ID, instance.getId());
    result.set(
        INSTANCE_JSON_VERSION,
        isNull(instance.getVersion()) ? mapper.nullNode() : IntNode.valueOf(instance.getVersion()));

    var details =
        rules.getBulkOperationRules().stream()
            .map(BulkOperationRule::getRuleDetails)
            .filter(Objects::nonNull)
            .toList();

    for (var ruleDetails : details) {
      if (ruleDetails.getOption() != null) {
        switch (ruleDetails.getOption()) {
          case STAFF_SUPPRESS ->
              result.set(
                  INSTANCE_JSON_STAFF_SUPPRESS,
                  isNull(instance.getStaffSuppress())
                      ? mapper.nullNode()
                      : BooleanNode.valueOf(instance.getStaffSuppress()));
          case SUPPRESS_FROM_DISCOVERY ->
              result.set(
                  INSTANCE_JSON_DISCOVERY_SUPPRESS,
                  isNull(instance.getDiscoverySuppress())
                      ? mapper.nullNode()
                      : BooleanNode.valueOf(instance.getDiscoverySuppress()));
          case SET_RECORDS_FOR_DELETE -> {
            result.set(
                INSTANCE_JSON_STAFF_SUPPRESS,
                isNull(instance.getStaffSuppress())
                    ? mapper.nullNode()
                    : BooleanNode.valueOf(instance.getStaffSuppress()));
            result.set(
                INSTANCE_JSON_DISCOVERY_SUPPRESS,
                isNull(instance.getDiscoverySuppress())
                    ? mapper.nullNode()
                    : BooleanNode.valueOf(instance.getDiscoverySuppress()));
            result.set(
                INSTANCE_JSON_DELETED,
                isNull(instance.getDeleted())
                    ? mapper.nullNode()
                    : BooleanNode.valueOf(instance.getDeleted()));
          }
          case STATISTICAL_CODE ->
              result.set(
                  INSTANCE_JSON_STATISTICAL_CODES,
                  mapper.valueToTree(instance.getStatisticalCodeIds()));
          case ADMINISTRATIVE_NOTE -> {
            result.set(
                INSTANCE_JSON_ADM_NOTES, mapper.valueToTree(instance.getAdministrativeNotes()));
            if (CHANGE_TYPE.equals(ruleDetails.getActions().getFirst().getType())) {
              result.set(INSTANCE_JSON_NOTES, mapper.valueToTree(instance.getInstanceNotes()));
            }
          }
          case INSTANCE_NOTE -> {
            result.set(INSTANCE_JSON_NOTES, mapper.valueToTree(instance.getInstanceNotes()));
            if (ruleDetails.getActions().getFirst().getType().equals(CHANGE_TYPE)
                && ADMINISTRATIVE_NOTE
                    .getValue()
                    .equals(ruleDetails.getActions().getFirst().getUpdated())) {
              result.set(
                  INSTANCE_JSON_ADM_NOTES, mapper.valueToTree(instance.getAdministrativeNotes()));
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
}
