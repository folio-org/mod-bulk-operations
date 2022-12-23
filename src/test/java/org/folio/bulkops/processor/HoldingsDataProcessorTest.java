package org.folio.bulkops.processor;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecordsSource;
import org.folio.bulkops.domain.bean.ItemLocation;
import org.folio.bulkops.domain.dto.Action;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.BulkOperationRuleRuleDetails;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.domain.dto.UpdateOptionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;

import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class HoldingsDataProcessorTest extends BaseTest {

  @Autowired
  HoldingsDataProcessor processor;

  @Test
  public void rulesValidationTest() {

    var original = getOriginalHolding();


//    when(locationClient.getLocationById(any())).thenReturn(new ItemLocation()
//      .withId("Annex").withName("Annex"));
    when(holdingsSourceClient.getById(any())).thenReturn(new HoldingsRecordsSource()
      .withName("MARC"));

    var rules = rules(rule(PERMANENT_LOCATION, REPLACE_WITH, "Annex"));

    assertNull(processor.process(original, rules));

    assertNull(processor.process(original, rules(rule(PERMANENT_LOCATION, REPLACE_WITH, null))));

    assertNull(processor.process(original, rules(rule(PERMANENT_LOCATION, REPLACE_WITH, "non-existed"))));
  }

  private static HoldingsRecord getOriginalHolding() {
    var id = "6272571f-e9ff-4232-9d0e-864ab2771da8";
    return new HoldingsRecord()
      .withEffectiveLocationId("b40bc179-596b-4d6a-bfcd-6d9bd8619458")
      .withPermanentLocation(new ItemLocation().withId(id).withName("Annex"));
  }

//  @ParameterizedTest
//  @EnumSource(HoldingsContentUpdateValidTestData.class)
//  void shouldAllowValidContentUpdateData(HoldingsContentUpdateValidTestData testData) {
//    assertTrue(validatorService.validateContentUpdateCollection(testData.getRulesCollection()));
//  }

  private static BulkOperationRuleCollection rules(BulkOperationRule... rules) {
    return new BulkOperationRuleCollection()
      .bulkOperationRules(Arrays.asList(rules))
      .totalRecords(rules.length);
  }

  private static BulkOperationRule rule(UpdateOptionType option, UpdateActionType action, String value) {
    return new BulkOperationRule()
      .ruleDetails(new BulkOperationRuleRuleDetails()
        .option(option)
        .actions(Collections.singletonList(new Action()
          .type(action)
          .updated(value))));
  }
}
