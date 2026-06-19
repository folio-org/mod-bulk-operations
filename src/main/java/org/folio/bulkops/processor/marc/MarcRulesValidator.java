package org.folio.bulkops.processor.marc;

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

@Component
public class MarcRulesValidator {

  private static final Pattern TAG_PATTERN = Pattern.compile("(\\d{3}|LDR)");
  private static final Pattern UNSUPPORTED_TAG_PATTERN = Pattern.compile("00\\d");
  private static final String NOT_SUPPORTED_BULK_EDIT_FIELD_MESSAGE =
      "Bulk edit of %s field is not supported";
  static final String MISSING_REQUIRED_FIELD_MESSAGE = "Missing required field %s.";

  public void validate(BulkOperationMarcRule bulkOperationMarcRule) throws RuleValidationException {
    var tag = bulkOperationMarcRule.getTag();
    if (StringUtils.isEmpty(tag)
        || !TAG_PATTERN.matcher(tag).matches()
        || UNSUPPORTED_TAG_PATTERN.matcher(tag).matches()) {
      throw new RuleValidationException(String.format(NOT_SUPPORTED_BULK_EDIT_FIELD_MESSAGE, tag));
    }
  }

  /**
   * Validates that all required fields are present for REMOVE_FIELD and REMOVE_SUBFIELD operations.
   *
   * <ul>
   *   <li>REMOVE_FIELD requires: tag, ind1, ind2
   *   <li>REMOVE_SUBFIELD requires: tag, ind1, ind2, subfield
   * </ul>
   *
   * @throws BadRequestException with message "Missing required field &lt;name&gt;." if a required
   *     field is absent
   */
  public void validateRequiredFields(BulkOperationMarcRule rule) {
    if (rule.getActions() == null || rule.getActions().isEmpty()) {
      return;
    }
    var firstAction = rule.getActions().getFirst().getName();
    if (UpdateActionType.REMOVE_FIELD.equals(firstAction)) {
      validateRemoveFieldRequiredFields(rule);
    } else if (UpdateActionType.REMOVE_SUBFIELD.equals(firstAction)) {
      validateRemoveSubfieldRequiredFields(rule);
    }
  }

  private void validateRemoveFieldRequiredFields(BulkOperationMarcRule rule) {
    if (StringUtils.isEmpty(rule.getTag())) {
      throw new BadRequestException(String.format(MISSING_REQUIRED_FIELD_MESSAGE, "tag"));
    }
    if (isNull(rule.getInd1())) {
      throw new BadRequestException(String.format(MISSING_REQUIRED_FIELD_MESSAGE, "ind1"));
    }
    if (isNull(rule.getInd2())) {
      throw new BadRequestException(String.format(MISSING_REQUIRED_FIELD_MESSAGE, "ind2"));
    }
  }

  private void validateRemoveSubfieldRequiredFields(BulkOperationMarcRule rule) {
    validateRemoveFieldRequiredFields(rule);
    if (StringUtils.isEmpty(rule.getSubfield())) {
      throw new BadRequestException(String.format(MISSING_REQUIRED_FIELD_MESSAGE, "subfield"));
    }
  }
}
