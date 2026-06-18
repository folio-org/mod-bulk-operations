package org.folio.bulkops.processor.marc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.MarcAction;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.exception.BadRequestException;
import org.folio.bulkops.exception.RuleValidationException;
import org.junit.jupiter.api.Test;

class MarcRulesValidatorTest {

  private final MarcRulesValidator validator = new MarcRulesValidator();

  @Test
  void testValidate() {
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

  // --- REMOVE_FIELD validation ---

  @Test
  void validateRequiredFields_removeField_missingTag_throwsBadRequest() {
    var rule =
        new BulkOperationMarcRule()
            .ind1("1")
            .ind2("1")
            .actions(List.of(new MarcAction().name(UpdateActionType.REMOVE_FIELD)));
    var ex = assertThrows(BadRequestException.class, () -> validator.validateRequiredFields(rule));
    assertEquals(
        String.format(MarcRulesValidator.MISSING_REQUIRED_FIELD_MESSAGE, "tag"), ex.getMessage());
  }

  @Test
  void validateRequiredFields_removeField_missingInd1_throwsBadRequest() {
    var rule =
        new BulkOperationMarcRule()
            .tag("500")
            .ind2("1")
            .actions(List.of(new MarcAction().name(UpdateActionType.REMOVE_FIELD)));
    var ex = assertThrows(BadRequestException.class, () -> validator.validateRequiredFields(rule));
    assertEquals(
        String.format(MarcRulesValidator.MISSING_REQUIRED_FIELD_MESSAGE, "ind1"), ex.getMessage());
  }

  @Test
  void validateRequiredFields_removeField_missingInd2_throwsBadRequest() {
    var rule =
        new BulkOperationMarcRule()
            .tag("500")
            .ind1("1")
            .actions(List.of(new MarcAction().name(UpdateActionType.REMOVE_FIELD)));
    var ex = assertThrows(BadRequestException.class, () -> validator.validateRequiredFields(rule));
    assertEquals(
        String.format(MarcRulesValidator.MISSING_REQUIRED_FIELD_MESSAGE, "ind2"), ex.getMessage());
  }

  @Test
  void validateRequiredFields_removeField_subfieldNotRequired_passes() {
    var rule =
        new BulkOperationMarcRule()
            .tag("500")
            .ind1("1")
            .ind2("1")
            .actions(List.of(new MarcAction().name(UpdateActionType.REMOVE_FIELD)));
    assertDoesNotThrow(() -> validator.validateRequiredFields(rule));
  }

  // --- REMOVE_SUBFIELD validation ---

  @Test
  void validateRequiredFields_removeSubfield_missingSubfield_throwsBadRequest() {
    var rule =
        new BulkOperationMarcRule()
            .tag("500")
            .ind1("1")
            .ind2("1")
            .actions(List.of(new MarcAction().name(UpdateActionType.REMOVE_SUBFIELD)));
    var ex = assertThrows(BadRequestException.class, () -> validator.validateRequiredFields(rule));
    assertEquals(
        String.format(MarcRulesValidator.MISSING_REQUIRED_FIELD_MESSAGE, "subfield"),
        ex.getMessage());
  }

  @Test
  void validateRequiredFields_removeSubfield_allPresent_passes() {
    var rule =
        new BulkOperationMarcRule()
            .tag("500")
            .ind1("1")
            .ind2("1")
            .subfield("a")
            .actions(List.of(new MarcAction().name(UpdateActionType.REMOVE_SUBFIELD)));
    assertDoesNotThrow(() -> validator.validateRequiredFields(rule));
  }

  @Test
  void validateRequiredFields_removeSubfield_missingInd1_throwsBadRequest() {
    var rule =
        new BulkOperationMarcRule()
            .tag("500")
            .ind2("1")
            .subfield("a")
            .actions(List.of(new MarcAction().name(UpdateActionType.REMOVE_SUBFIELD)));
    var ex = assertThrows(BadRequestException.class, () -> validator.validateRequiredFields(rule));
    assertEquals(
        String.format(MarcRulesValidator.MISSING_REQUIRED_FIELD_MESSAGE, "ind1"), ex.getMessage());
  }

  // --- Other actions are not validated ---

  @Test
  void validateRequiredFields_otherAction_noValidation() {
    // Other actions (e.g. FIND, ADD_TO_EXISTING) do not trigger required-field validation
    var rule =
        new BulkOperationMarcRule()
            .tag("500")
            .ind1("1")
            .ind2("1")
            .actions(List.of(new MarcAction().name(UpdateActionType.ADD_TO_EXISTING)));
    assertDoesNotThrow(() -> validator.validateRequiredFields(rule));
  }

  @Test
  void validateRequiredFields_emptyActions_noValidation() {
    var rule = new BulkOperationMarcRule().tag("500").ind1("1").ind2("1");
    assertDoesNotThrow(() -> validator.validateRequiredFields(rule));
  }
}
