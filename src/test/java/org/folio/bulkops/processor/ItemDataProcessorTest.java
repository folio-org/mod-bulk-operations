package org.folio.bulkops.processor;

import org.folio.bulkops.BaseTest;
import org.folio.bulkops.client.HoldingsSourceClient;
import org.folio.bulkops.client.LocationClient;
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
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.Collections;

import static org.folio.bulkops.domain.dto.UpdateActionType.REPLACE_WITH;
import static org.folio.bulkops.domain.dto.UpdateOptionType.PERMANENT_LOCATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ItemDataProcessorTest extends BaseTest {

  @Autowired
  HoldingsDataProcessor processor;

  @Test
  public void test() {
    var original = new HoldingsRecord()
      .withEffectiveLocationId("Annex")
      .withPermanentLocation(new ItemLocation().withId("Annex").withName("Annex"));


    when(locationClient.getLocationById(any())).thenReturn(new ItemLocation()
      .withId("Annex").withName("Annex"));
    when(holdingsSourceClient.getById(any())).thenReturn(new HoldingsRecordsSource()
      .withName("MARC"));



    var rules = rules(rule(PERMANENT_LOCATION, REPLACE_WITH, "Annex"));
    var result = processor.process(original, rules);
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
