package org.folio.bulkops.processor;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.util.Constants.APPLY_TO_ITEMS;
import static org.folio.bulkops.util.Constants.GET_ITEMS_BY_HOLDING_ID_QUERY;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;
import static org.folio.bulkops.util.RuleUtils.fetchParameters;
import static org.folio.bulkops.util.RuleUtils.findRuleByOption;

import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.RuleService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HoldingsUpdateProcessor extends AbstractUpdateProcessor<ExtendedHoldingsRecord> {
  private static final String ERROR_MESSAGE_TEMPLATE = "No change in value for holdings record required, associated %s item(s) have been updated.";

  private final HoldingsClient holdingsClient;
  private final ItemClient itemClient;
  private final RuleService ruleService;
  private final ErrorService errorService;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final ConsortiaService consortiaService;

  @Override
  public void updateRecord(ExtendedHoldingsRecord extendedHoldingsRecord) {
    var holdingsRecord = extendedHoldingsRecord.getEntity();
    if (consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      var tenantId = extendedHoldingsRecord.getTenantId();
      try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
        holdingsClient.updateHoldingsRecord(
          holdingsRecord.withInstanceHrid(null).withItemBarcode(null).withInstanceTitle(null),
          holdingsRecord.getId()
        );
      }
    } else {
      holdingsClient.updateHoldingsRecord(
        holdingsRecord.withInstanceHrid(null).withItemBarcode(null).withInstanceTitle(null),
        holdingsRecord.getId()
      );
    }
  }

  @Override
  public void updateAssociatedRecords(ExtendedHoldingsRecord extendedHoldingsRecord, BulkOperation operation, boolean notChanged) {
    var holdingsRecord = extendedHoldingsRecord.getEntity();
    var bulkOperationRules = ruleService.getRules(operation.getId());
    boolean itemsUpdated;
    if (consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      var tenantId = extendedHoldingsRecord.getTenantId();
      try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
        itemsUpdated = findRuleByOption(bulkOperationRules, SUPPRESS_FROM_DISCOVERY)
          .filter(bulkOperationRule -> suppressItemsIfRequired(holdingsRecord, bulkOperationRule))
          .isPresent();
      }
    } else {
      itemsUpdated = findRuleByOption(bulkOperationRules, SUPPRESS_FROM_DISCOVERY)
        .filter(bulkOperationRule -> suppressItemsIfRequired(holdingsRecord, bulkOperationRule))
        .isPresent();
    }
    if (notChanged) {
      var errorMessage = buildErrorMessage(itemsUpdated, holdingsRecord.getDiscoverySuppress());
      errorService.saveError(operation.getId(), holdingsRecord.getIdentifier(operation.getIdentifierType()), errorMessage);
    }
  }

  private boolean suppressItemsIfRequired(HoldingsRecord holdingsRecord, BulkOperationRule rule) {
    List<Item> itemsForUpdate = parseBoolean(fetchParameters(rule).get(APPLY_TO_ITEMS)) ?
      itemClient.getByQuery(format(GET_ITEMS_BY_HOLDING_ID_QUERY, holdingsRecord.getId()), Integer.MAX_VALUE)
        .getItems().stream()
        .filter(item -> !holdingsRecord.getDiscoverySuppress().equals(item.getDiscoverySuppress()))
        .toList() :
      Collections.emptyList();
    if (itemsForUpdate.isEmpty()) {
      return false;
    }
    itemsForUpdate.forEach(item -> itemClient.updateItem(item.withDiscoverySuppress(holdingsRecord.getDiscoverySuppress()), item.getId()));
    return true;
  }

  private String buildErrorMessage(boolean itemsUpdated, boolean newValue) {
    var affectedState = TRUE.equals(newValue) ? "unsuppressed" : "suppressed";
    return itemsUpdated ?
      format(ERROR_MESSAGE_TEMPLATE, affectedState) :
      MSG_NO_CHANGE_REQUIRED;
  }

  @Override
  public Class<ExtendedHoldingsRecord> getUpdatedType() {
    return ExtendedHoldingsRecord.class;
  }
}
