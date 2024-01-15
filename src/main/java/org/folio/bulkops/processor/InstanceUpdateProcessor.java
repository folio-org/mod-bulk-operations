package org.folio.bulkops.processor;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.util.Constants.APPLY_TO_HOLDINGS;
import static org.folio.bulkops.util.Constants.APPLY_TO_ITEMS;
import static org.folio.bulkops.util.Constants.GET_HOLDINGS_BY_INSTANCE_ID_QUERY;
import static org.folio.bulkops.util.Constants.GET_ITEMS_BY_HOLDING_ID_QUERY;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;
import static org.folio.bulkops.util.RuleUtils.fetchParameters;
import static org.folio.bulkops.util.RuleUtils.findRuleByOption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.InstanceStorageClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.SRSRecordsClient;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemCollection;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.RuleService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstanceUpdateProcessor extends AbstractUpdateProcessor<Instance> {
  private static final String ERROR_MESSAGE_TEMPLATE = "No change in value for instance required, %s associated records have been updated.";

  private final InstanceStorageClient instanceStorageClient;
  private final RuleService ruleService;
  private final SRSRecordsClient srsRecordsClient;
  private final HoldingsClient holdingsClient;
  private final ItemClient itemClient;
  private final ErrorService errorService;

  @Override
  public void updateRecord(Instance instance) {
    instanceStorageClient.updateInstance(instance.withIsbn(null).withIssn(null), instance.getId());
  }

  @Override
  public void updateAssociatedRecords(Instance instance, BulkOperation operation, boolean notChanged) {
    var recordsUpdated = findRuleByOption(ruleService.getRules(operation.getId()), SUPPRESS_FROM_DISCOVERY)
      .filter(rule -> applyRuleToAssociatedRecords(instance, rule))
      .isPresent();
    if (notChanged) {
      var errorMessage = buildErrorMessage(recordsUpdated, instance.getDiscoverySuppress());
      errorService.saveError(operation.getId(), instance.getIdentifier(operation.getIdentifierType()), errorMessage);
    }
  }

  private boolean applyRuleToAssociatedRecords(Instance instance, BulkOperationRule rule) {
    var srsUpdated = suppressSRSRecordIfRequired(instance);
    var parameters = fetchParameters(rule);
    var shouldApplyToHoldings = parseBoolean(parameters.get(APPLY_TO_HOLDINGS));
    var shouldApplyToItems = parseBoolean(parameters.get(APPLY_TO_ITEMS));
    boolean holdingsUpdated = false;
    boolean itemsUpdated = false;
    if (shouldApplyToHoldings || shouldApplyToItems) {
      log.info("Should update associated records: holdings={}, items={}", shouldApplyToHoldings, shouldApplyToItems);
      var holdings = holdingsClient.getByQuery(format(GET_HOLDINGS_BY_INSTANCE_ID_QUERY, instance.getId()), Integer.MAX_VALUE).getHoldingsRecords();
      holdingsUpdated = suppressHoldingsIfRequired(holdings, shouldApplyToHoldings, instance.getDiscoverySuppress());
      itemsUpdated = suppressItemsIfRequired(holdings, shouldApplyToItems, instance.getDiscoverySuppress());
    }
    return srsUpdated || holdingsUpdated || itemsUpdated;
  }

  private boolean suppressSRSRecordIfRequired(Instance instance) {
    if ("MARC".equals(instance.getSource())) {
      srsRecordsClient.setSuppressFromDiscovery(instance.getId(), "INSTANCE", instance.getDiscoverySuppress());
      log.info("Updated underlying SRS record for instance {}: suppress from discovery -> {}", instance.getId(), instance.getDiscoverySuppress());
      return true;
    }
    return false;
  }

  private boolean suppressHoldingsIfRequired(List<HoldingsRecord> holdingsRecords, boolean applyToHoldings, boolean suppress) {
    List<HoldingsRecord> holdingsForUpdate = applyToHoldings ?
      holdingsRecords.stream()
        .filter(holdingsRecord -> holdingsRecord.getDiscoverySuppress() != suppress)
        .toList() :
      Collections.emptyList();
    log.info("Found {} holdings for update", holdingsForUpdate.size());
    if (holdingsForUpdate.isEmpty()) {
      return false;
    }
    holdingsForUpdate.forEach(holdingsRecord -> holdingsClient.updateHoldingsRecord(holdingsRecord.withDiscoverySuppress(suppress), holdingsRecord.getId()));
    return true;
  }

  private boolean suppressItemsIfRequired(List<HoldingsRecord> holdingsRecords, boolean applyToItems, boolean suppress) {
    List<Item> itemsForUpdate = applyToItems ?
      holdingsRecords.stream()
        .map(HoldingsRecord::getId)
        .map(id -> itemClient.getByQuery(format(GET_ITEMS_BY_HOLDING_ID_QUERY, id), Integer.MAX_VALUE))
        .map(ItemCollection::getItems)
        .flatMap(List::stream)
        .filter(item -> suppress != item.getDiscoverySuppress())
        .toList() :
      Collections.emptyList();
    log.info("Found {} items for update", itemsForUpdate.size());
    if (itemsForUpdate.isEmpty()) {
      return false;
    }
    itemsForUpdate.forEach(item -> itemClient.updateItem(item.withDiscoverySuppress(suppress), item.getId()));
    return true;
  }

  private String buildErrorMessage(boolean recordsUpdated, boolean newValue) {
    var affectedState = TRUE.equals(newValue) ? "unsuppressed" : "suppressed";
    return recordsUpdated ?
      format(ERROR_MESSAGE_TEMPLATE, affectedState) :
      MSG_NO_CHANGE_REQUIRED;
  }

  @Override
  public Class<Instance> getUpdatedType() {
    return Instance.class;
  }
}
