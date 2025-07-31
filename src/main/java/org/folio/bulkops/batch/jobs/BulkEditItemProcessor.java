package org.folio.bulkops.batch.jobs;

import static org.folio.bulkops.domain.dto.BatchIdsDto.IdentifierTypeEnum.HOLDINGSRECORDID;
import static org.folio.bulkops.domain.dto.IdentifierType.HOLDINGS_RECORD_ID;
import static org.folio.bulkops.util.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.bulkops.util.BulkEditProcessorHelper.getResponseAsString;
import static org.folio.bulkops.util.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.bulkops.util.Constants.DUPLICATES_ACROSS_TENANTS;
import static org.folio.bulkops.util.Constants.MULTIPLE_MATCHES_MESSAGE;
import static org.folio.bulkops.util.Constants.NO_ITEM_VIEW_PERMISSIONS;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.bulkops.util.SearchIdentifierTypeResolver.getSearchIdentifierType;
import static org.folio.bulkops.util.Utils.encode;

import feign.codec.DecodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.batch.jobs.processidentifiers.DuplicationCheckerFactory;
import org.folio.bulkops.client.ItemClient;
import org.folio.bulkops.client.SearchClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ExtendedItem;
import org.folio.bulkops.domain.bean.ExtendedItemCollection;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.dto.BatchIdsDto;
import org.folio.bulkops.domain.dto.ConsortiumItem;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.processor.EntityExtractor;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.processor.permissions.check.TenantResolver;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.EntityDataHelper;
import org.folio.bulkops.service.LocalReferenceDataService;
import org.folio.bulkops.util.ExceptionHelper;
import org.folio.bulkops.util.FolioExecutionContextUtil;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemProcessor implements ItemProcessor<ItemIdentifier, ExtendedItemCollection>, EntityExtractor {
  private final ItemClient itemClient;
  private final ConsortiaService consortiaService;
  private final SearchClient searchClient;
  private final UserClient userClient;
  private final PermissionsValidator permissionsValidator;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private final TenantResolver tenantResolver;
  private final DuplicationCheckerFactory duplicationCheckerFactory;
  private final EntityDataHelper entityDataHelper;
  private final LocalReferenceDataService localReferenceDataService;

  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;
  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  @Override
  public ExtendedItemCollection process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (!duplicationCheckerFactory.getIdentifiersToCheckDuplication(jobExecution).add(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry", ErrorType.WARNING);
    }
    var type = IdentifierType.fromValue(identifierType);
    var limit = HOLDINGS_RECORD_ID.equals(type) ? Integer.MAX_VALUE : 1;
    var idType = resolveIdentifier(identifierType);
    var identifier = "barcode".equals(idType) ? encode(itemIdentifier.getItemId()) : itemIdentifier.getItemId();
    try {
      final ExtendedItemCollection extendedItemCollection = new ExtendedItemCollection()
        .withExtendedItems(new ArrayList<>())
        .withTotalRecords(0);
      var centralTenantId = consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());
      if (isCurrentTenantCentral(centralTenantId)) {
        // Assuming item is requested by only one identifier not a collection of identifiers
        var identifierTypeEnum = getSearchIdentifierType(type);
        var batchIdsDto = new BatchIdsDto()
          .identifierType(identifierTypeEnum)
          .identifierValues(List.of(itemIdentifier.getItemId()));
        var consortiumItemCollection = searchClient.getConsortiumItemCollection(batchIdsDto);
        if (consortiumItemCollection.getTotalRecords() > 0) {
          var tenantIds = consortiumItemCollection.getItems()
            .stream()
            .map(ConsortiumItem::getTenantId).collect(Collectors.toSet());
          if (HOLDINGSRECORDID != identifierTypeEnum && tenantIds.size() > 1) {
            throw new BulkEditException(DUPLICATES_ACROSS_TENANTS, ErrorType.ERROR);
          }
          var affiliatedPermittedTenants = tenantResolver.getAffiliatedPermittedTenantIds(EntityType.ITEM,
            jobExecution, identifierType, tenantIds, itemIdentifier);
          affiliatedPermittedTenants.forEach(tenantId -> {
            try (var context = new FolioExecutionContextSetter(FolioExecutionContextUtil.prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
              var url = getMatchPattern(identifierType).formatted(idType, identifier);
              var itemCollection = itemClient.getByQuery(url, Integer.MAX_VALUE);
              if (itemCollection.getItems().size() > limit) {
                log.error("Central tenant case: response from {} for tenant {}: {}", url, tenantId, getResponseAsString(itemCollection));
                throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE, ErrorType.ERROR);
              }
              extendedItemCollection.getExtendedItems().addAll(itemCollection.getItems().stream()
                      .map(item -> item.withTitle(entityDataHelper.getInstanceTitle(item.getHoldingsRecordId(), tenantId)))
                      .map(item -> item.withHoldingsData(entityDataHelper.getHoldingsData(item.getHoldingsRecordId(), tenantId)))
                .map(item -> {
                  localReferenceDataService.enrichWithTenant(item, tenantId);
                  return item.withTenantId(tenantId);
                })
                .map(item -> new ExtendedItem().withTenantId(tenantId).withEntity(item))
                .toList());
              extendedItemCollection.setTotalRecords(extendedItemCollection.getTotalRecords() + itemCollection.getTotalRecords());
            } catch (Exception e) {
              log.error(e.getMessage());
              throw e;
            }
          });
        } else {
          throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
        }
      } else {
        // Process local tenant case
        checkReadPermissions(folioExecutionContext.getTenantId(), identifier);
        var query = getMatchPattern(identifierType).formatted(idType, identifier);
        var currentTenantId = folioExecutionContext.getTenantId();
        var itemCollection =  itemClient.getByQuery(query, Integer.MAX_VALUE);
        if (itemCollection.getItems().size() > limit) {
          log.error("Member/local tenant case: response from {} for tenant {}: {}", query, currentTenantId, getResponseAsString(itemCollection));
          throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE, ErrorType.ERROR);
        }
        var tenantId = folioExecutionContext.getTenantId();
        extendedItemCollection.setExtendedItems(itemCollection.getItems().stream()
          .map(item -> item.withTitle(entityDataHelper.getInstanceTitle(item.getHoldingsRecordId(), tenantId)))
          .map(item -> item.withHoldingsData(entityDataHelper.getHoldingsData(item.getHoldingsRecordId(), tenantId)))
          .map(item -> new ExtendedItem().withTenantId(tenantId).withEntity(item)).toList());
        extendedItemCollection.setTotalRecords(itemCollection.getTotalRecords());
        if (extendedItemCollection.getExtendedItems().isEmpty()) {
          log.error(NO_MATCH_FOUND_MESSAGE);
          throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
        }
      }
      return extendedItemCollection;
    } catch (DecodeException e) {
      throw new BulkEditException(ExceptionHelper.fetchMessage(e), ErrorType.ERROR);
    }
  }

  private void checkReadPermissions(String tenantId, String identifier) {
    if (!permissionsValidator.isBulkEditReadPermissionExists(tenantId, EntityType.ITEM)) {
      var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
      throw new BulkEditException(NO_ITEM_VIEW_PERMISSIONS.formatted(user.getUsername(), resolveIdentifier(identifierType), identifier, tenantId), ErrorType.ERROR);
    }
  }

  private boolean isCurrentTenantCentral(String centralTenantId) {
    return StringUtils.isNotEmpty(centralTenantId) && centralTenantId.equals(folioExecutionContext.getTenantId());
  }
}
