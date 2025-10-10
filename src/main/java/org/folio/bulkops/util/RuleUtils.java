package org.folio.bulkops.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.Parameter;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.folio.spring.FolioExecutionContext;

@UtilityClass
public class RuleUtils {
  public static Optional<BulkOperationRule> findRuleByOption(BulkOperationRuleCollection rules,
                                                             UpdateOptionType option) {
    return rules.getBulkOperationRules().stream()
      .filter(rule -> option.equals(rule.getRuleDetails().getOption()))
      .findFirst();
  }

  public static Map<String, String> fetchParameters(BulkOperationRule rule) {
    return rule.getRuleDetails().getActions().stream()
      .map(Action::getParameters)
      .filter(Objects::nonNull)
      .flatMap(List::stream)
      .collect(Collectors.toMap(Parameter::getKey, Parameter::getValue, (
              existing, replacement) -> existing));
  }

  public static String getTenantFromAction(Action action,
                                           FolioExecutionContext folioExecutionContext) {
    var actionTenants = action.getTenants();
    var updatedTenants = action.getUpdatedTenants();
    if ((isNull(actionTenants) || actionTenants.isEmpty())
            && (isNull(updatedTenants) || updatedTenants.isEmpty())) {
      return folioExecutionContext.getTenantId();
    }
    return nonNull(actionTenants) && !actionTenants.isEmpty() ? actionTenants.getFirst()
            : updatedTenants.getFirst();
  }
}
