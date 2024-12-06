package org.folio.bulkops.processor.marc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.exception.RuleValidationException;
import org.junit.jupiter.api.Test;

class MarcRulesValidatorTest {

  @Test
  void testValidate() {
    var validator = new MarcRulesValidator();
    var bulkOperationMarcRule = new BulkOperationMarcRule();
    bulkOperationMarcRule.setTag(null);
    assertThrows(RuleValidationException.class, () -> validator.validate(bulkOperationMarcRule));
    bulkOperationMarcRule.setTag("50i");
    assertThrows(RuleValidationException.class, () -> validator.validate(bulkOperationMarcRule));
    bulkOperationMarcRule.setTag("9091");
    assertThrows(RuleValidationException.class, () -> validator.validate(bulkOperationMarcRule));

    bulkOperationMarcRule.setTag("509");
    assertDoesNotThrow(() -> validator.validate(bulkOperationMarcRule));
    bulkOperationMarcRule.setTag("909");
    assertDoesNotThrow(() -> validator.validate(bulkOperationMarcRule));
  }
}
