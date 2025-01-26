package org.folio.bulkops.processor.folio;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.UpdateOptionType.SUPPRESS_FROM_DISCOVERY;
import static org.folio.bulkops.util.Constants.APPLY_TO_HOLDINGS;
import static org.folio.bulkops.util.Constants.APPLY_TO_ITEMS;
import static org.folio.bulkops.util.Constants.GET_HOLDINGS_BY_INSTANCE_ID_QUERY;
import static org.folio.bulkops.util.Constants.GET_ITEMS_BY_HOLDING_ID_QUERY;
import static org.folio.bulkops.util.Constants.MARC;
import static org.folio.bulkops.util.Constants.MSG_NO_CHANGE_REQUIRED;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;
import static org.folio.bulkops.util.RuleUtils.fetchParameters;
import static org.folio.bulkops.util.RuleUtils.findRuleByOption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.folio.bulkops.client.HoldingsClient;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.SearchConsortium;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ConsortiumHolding;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.bean.HoldingsRecord;
import org.folio.bulkops.domain.bean.Item;
import org.folio.bulkops.domain.bean.ItemCollection;
import org.folio.bulkops.domain.dto.BulkOperationRule;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.entity.BulkOperation;
import org.folio.bulkops.processor.FolioAbstractUpdateProcessor;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ErrorService;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.RuleService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FolioInstanceUpdateProcessor extends FolioAbstractUpdateProcessor<ExtendedInstance> {
  private static final String ERROR_MESSAGE_TEMPLATE = "No change in value for instance required, %s associated records have been updated.";
  private static final String ERROR_NO_AFFILIATION_TO_EDIT_HOLDINGS = "User %s does not have required affiliation to edit the holdings record - %s on the tenant %s";
  private static final String NO_INSTANCE_WRITE_PERMISSIONS_TEMPLATE = "User %s does not have required permission to edit the instance record - %s=%s on the tenant ";

  private final InstanceClient instanceClient;
  private final UserClient userClient;
  private final RuleService ruleService;
  private final HoldingsClient holdingsClient;
  private final ItemClient itemClient;
  private final SearchConsortium searchConsortium;
  private final ErrorService errorService;
  private final HoldingsReferenceService holdingsReferenceService;
  private final ConsortiaService consortiaService;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final PermissionsValidator permissionsValidator;

  @Override
  public void updateRecord(ExtendedInstance extendedInstance) {
    permissionsValidator.checkIfBulkEditWritePermissionExists(extendedInstance.getTenantId(), EntityType.INSTANCE,
      NO_INSTANCE_WRITE_PERMISSIONS_TEMPLATE + extendedInstance.getTenantId());
    var instance = extendedInstance.getEntity();
    instanceClient.updateInstance(instance.withIsbn(null).withIssn(null), instance.getId());
  }

  @Override
  public void updateAssociatedRecords(ExtendedInstance extendedInstance, BulkOperation operation, boolean notChanged) {
    var instance = extendedInstance.getEntity();
    var recordsUpdated = findRuleByOption(ruleService.getRules(operation.getId()), SUPPRESS_FROM_DISCOVERY)
      .filter(rule -> applyRuleToAssociatedRecords(extendedInstance, rule, operation))
      .isPresent();
    if (notChanged) {
      var errorMessage = buildErrorMessage(recordsUpdated, instance.getDiscoverySuppress());
      errorService.saveError(operation.getId(), instance.getIdentifier(operation.getIdentifierType()), errorMessage, ErrorType.WARNING);
    }
  }

  private boolean applyRuleToAssociatedRecords(ExtendedInstance extendedInstance, BulkOperationRule rule, BulkOperation operation) {
    var parameters = fetchParameters(rule);
    var shouldApplyToHoldings = parseBoolean(parameters.get(APPLY_TO_HOLDINGS));
    var shouldApplyToItems = parseBoolean(parameters.get(APPLY_TO_ITEMS));
    boolean holdingsUpdated = false;
    boolean itemsUpdated = false;
    if (shouldApplyToHoldings || shouldApplyToItems) {
      log.info("Should update associated records: holdings={}, items={}", shouldApplyToHoldings, shouldApplyToItems);
      if (!consortiaService.isTenantCentral(folioExecutionContext.getTenantId())) {
        var instance = extendedInstance.getEntity();
        var holdings = getHoldingsSourceFolioByInstanceId(instance.getId());
        holdingsUpdated = suppressHoldingsIfRequired(holdings, shouldApplyToHoldings, instance.getDiscoverySuppress());
        itemsUpdated = suppressItemsIfRequired(holdings, shouldApplyToItems, instance.getDiscoverySuppress());
      } else {
        var instance = extendedInstance.getEntity();
        var consortiumHoldings = searchConsortium.getHoldingsById(UUID.fromString(instance.getId())).getHoldings();
        Map<String, List<String>> consortiaHoldingsIdsPerTenant = consortiumHoldings.stream()
          .filter(h -> !folioExecutionContext.getTenantId().equals(h.getTenantId()))
          .collect(Collectors.groupingBy(ConsortiumHolding::getTenantId, Collectors.mapping(ConsortiumHolding::getId, Collectors.toList())));
        var userTenants = consortiaService.getAffiliatedTenants(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
        var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
        for (var consortiaHoldingsEntry : consortiaHoldingsIdsPerTenant.entrySet()) {
          var memberTenantForHoldings = consortiaHoldingsEntry.getKey();
          if (!userTenants.contains(memberTenantForHoldings)) {
            for (var holdingId : consortiaHoldingsEntry.getValue()) {
              var errorMessage = String.format(ERROR_NO_AFFILIATION_TO_EDIT_HOLDINGS, user.getUsername(), holdingId, memberTenantForHoldings);
              log.error(errorMessage);
              errorService.saveError(operation.getId(), instance.getIdentifier(operation.getIdentifierType()), errorMessage, ErrorType.ERROR);
            }
            continue;
          }
          try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(memberTenantForHoldings, folioModuleMetadata, folioExecutionContext))) {
            var holdings = getHoldingsSourceFolioByInstanceId(instance.getId());
            var isHoldingsUpdatedInMemberTenant = suppressHoldingsIfRequired(holdings, shouldApplyToHoldings, instance.getDiscoverySuppress());
            if (!holdingsUpdated) {
              holdingsUpdated = isHoldingsUpdatedInMemberTenant;
            }
            var isItemUpdatedInMemberTenant  = suppressItemsIfRequired(holdings, shouldApplyToItems, instance.getDiscoverySuppress());
            if (!itemsUpdated) {
              itemsUpdated = isItemUpdatedInMemberTenant;
            }
          }
        }
      }
    }
    return holdingsUpdated || itemsUpdated;
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

  private List<HoldingsRecord> getHoldingsSourceFolioByInstanceId(String instanceId) {
    return holdingsClient.getByQuery(format(GET_HOLDINGS_BY_INSTANCE_ID_QUERY, instanceId), Integer.MAX_VALUE)
      .getHoldingsRecords().stream()
      .filter(holdingsRecord -> !MARC.equals(holdingsReferenceService.getSourceById(holdingsRecord.getSourceId(), folioExecutionContext.getTenantId()).getName()))
      .toList();
  }

  @Override
  public Class<ExtendedInstance> getUpdatedType() {
    return ExtendedInstance.class;
  }
}
