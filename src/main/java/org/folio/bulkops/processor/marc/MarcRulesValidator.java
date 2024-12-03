package org.folio.bulkops.processor.marc;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

@Component
public class MarcRulesValidator {

  private static final String VALID_TAG_REGEXP = "^[59]\\d{2}$";
  private static final String NOT_SUPPORTED_BULK_EDIT_FIELD_MESSAGE = "Bulk edit of %s field is not supported";

  public void validate(BulkOperationMarcRule bulkOperationMarcRule) throws RuleValidationException {
    var tag = bulkOperationMarcRule.getTag();
    if (StringUtils.isEmpty(tag) || StringUtils.isNotEmpty(tag) && !tag.matches(VALID_TAG_REGEXP)) {
      throw new RuleValidationException(String.format(NOT_SUPPORTED_BULK_EDIT_FIELD_MESSAGE, tag));
    }
  }
}
