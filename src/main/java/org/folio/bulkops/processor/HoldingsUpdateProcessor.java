package org.folio.bulkops.processor;

import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.dto.BulkOperationRuleCollection;
import org.folio.bulkops.domain.dto.UpdateActionType;
import org.folio.bulkops.service.RuleService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;

@Component
@RequiredArgsConstructor
public class HoldingsUpdateProcessor implements UpdateProcessor<HoldingsRecord> {

  private static final String GET_ITEMS_BY_HOLDING_ID_QUERY = "query=holdingsRecordId=%s";

  private final HoldingsClient holdingsClient;
  private final ItemClient itemClient;
  private final RuleService ruleService;

  @Override
  public void updateRecord(HoldingsRecord holdingsRecord, UUID operationId) {
    var ruleCollection = ruleService.getRules(operationId);
    holdingsClient.updateHoldingsRecord(
      holdingsRecord.withInstanceHrid(null).withItemBarcode(null).withInstanceTitle(null),
      holdingsRecord.getId()
    );
    if (isNeedToUpdateItemsDiscoverySupressValue(ruleCollection)) {
      var items = itemClient.getByQuery(String.format(GET_ITEMS_BY_HOLDING_ID_QUERY, holdingsRecord.getId()), 0, 1000000)
        .getItems();
      items.forEach(item -> {
        item.setDiscoverySuppress(holdingsRecord.getDiscoverySuppress());
        itemClient.updateItem(item, item.getId());
      });
    }
  }

  private boolean isNeedToUpdateItemsDiscoverySupressValue(BulkOperationRuleCollection ruleCollection) {
    return ruleCollection.getBulkOperationRules().stream().anyMatch(rule -> {
      var ruleDetails = rule.getRuleDetails();
      var option = ruleDetails.getOption();
      if (option != SUPPRESS_FROM_DISCOVERY) return false;
      return ruleDetails.getActions().stream().anyMatch(action -> action.getType() == UpdateActionType.SET_TO_TRUE_INCLUDING_ITEMS ||
        action.getType() == UpdateActionType.SET_TO_FALSE_INCLUDING_ITEMS);
    });
  }

  @Override
  public Class<HoldingsRecord> getUpdatedType() {
    return HoldingsRecord.class;
  }
}
