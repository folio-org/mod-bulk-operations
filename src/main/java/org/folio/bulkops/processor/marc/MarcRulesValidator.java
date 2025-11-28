package org.folio.bulkops.processor.marc;

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.exception.RuleValidationException;
import org.springframework.stereotype.Component;

@Component
public class MarcRulesValidator {

  private static final Pattern TAG_PATTERN = Pattern.compile("(\\d{3}|LDR)");
  private static final Pattern UNSUPPORTED_TAG_PATTERN = Pattern.compile("00\\d");
  private static final String NOT_SUPPORTED_BULK_EDIT_FIELD_MESSAGE =
      "Bulk edit of %s field is not supported";

  public void validate(BulkOperationMarcRule bulkOperationMarcRule) throws RuleValidationException {
    var tag = bulkOperationMarcRule.getTag();
    if (StringUtils.isEmpty(tag)
        || !TAG_PATTERN.matcher(tag).matches()
        || UNSUPPORTED_TAG_PATTERN.matcher(tag).matches()) {
      throw new RuleValidationException(String.format(NOT_SUPPORTED_BULK_EDIT_FIELD_MESSAGE, tag));
    }
  }
}
