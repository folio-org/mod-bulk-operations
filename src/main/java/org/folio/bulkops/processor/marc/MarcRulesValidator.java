package org.folio.bulkops.processor.marc;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class MarcRulesValidator {

  private static final Pattern VALID_TAG_PATTERN = Pattern.compile("[1-9][1-9]\\d");
  private static final String NOT_SUPPORTED_BULK_EDIT_FIELD_MESSAGE = "Bulk edit of %s field is not supported";

  public void validate(BulkOperationMarcRule bulkOperationMarcRule) throws RuleValidationException {
    var tag = bulkOperationMarcRule.getTag();
    if (StringUtils.isEmpty(tag) || !VALID_TAG_PATTERN.matcher(tag).matches()) {
      throw new RuleValidationException(String.format(NOT_SUPPORTED_BULK_EDIT_FIELD_MESSAGE, tag));
    }
  }
}
