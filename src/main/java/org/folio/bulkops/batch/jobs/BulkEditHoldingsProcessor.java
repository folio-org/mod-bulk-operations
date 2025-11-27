package org.folio.bulkops.batch.jobs;

import static java.lang.String.format;
import static org.folio.bulkops.domain.dto.BatchIdsDto.IdentifierTypeEnum.INSTANCEHRID;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.IdentifierType.HRID;
import static org.folio.bulkops.domain.dto.IdentifierType.ID;
import static org.folio.bulkops.domain.dto.IdentifierType.INSTANCE_HRID;
import static org.folio.bulkops.domain.dto.IdentifierType.ITEM_BARCODE;
import static org.folio.bulkops.util.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.bulkops.util.BulkEditProcessorHelper.getResponseAsString;
import static org.folio.bulkops.util.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.bulkops.util.Constants.DUPLICATES_ACROSS_TENANTS;
import static org.folio.bulkops.util.Constants.MULTIPLE_MATCHES_MESSAGE;
import static org.folio.bulkops.util.Constants.NO_HOLDING_VIEW_PERMISSIONS;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.bulkops.util.FolioExecutionContextUtil.prepareContextForTenant;
import static org.folio.bulkops.util.SearchIdentifierTypeResolver.getSearchIdentifierType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.batch.jobs.processidentifiers.DuplicationCheckerFactory;
import org.folio.bulkops.client.HoldingsStorageClient;
import org.folio.bulkops.client.SearchClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecord;
import org.folio.bulkops.domain.bean.ExtendedHoldingsRecordCollection;
import org.folio.bulkops.domain.bean.HoldingsRecordCollection;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.dto.BatchIdsDto;
import org.folio.bulkops.domain.dto.ConsortiumHolding;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.processor.EntityExtractor;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.processor.permissions.check.TenantResolver;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.HoldingsReferenceService;
import org.folio.bulkops.service.LocalReferenceDataService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditHoldingsProcessor
    implements ItemProcessor<ItemIdentifier, List<ExtendedHoldingsRecord>>, EntityExtractor {
  private final HoldingsStorageClient holdingsStorageClient;
  private final HoldingsReferenceService holdingsReferenceService;
  private final SearchClient searchClient;
  private final ConsortiaService consortiaService;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private final UserClient userClient;
  private final PermissionsValidator permissionsValidator;
  private final TenantResolver tenantResolver;
  private final DuplicationCheckerFactory duplicationCheckerFactory;
  private final LocalReferenceDataService localReferenceDataService;

  @SuppressWarnings("unused")
  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;

  @SuppressWarnings("unused")
  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  @SuppressWarnings("unused")
  @Value("#{jobParameters['jobId']}")
  private String jobId;

  @SuppressWarnings("unused")
  @Value("#{jobParameters['fileName']}")
  private String fileName;

  @Override
  public List<ExtendedHoldingsRecord> process(@NotNull ItemIdentifier itemIdentifier)
      throws BulkEditException {
    if (!duplicationCheckerFactory
        .getIdentifiersToCheckDuplication(jobExecution)
        .add(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry", ErrorType.WARNING);
    }

    var holdings = getHoldingsRecords(itemIdentifier);

    var distinctHoldings =
        holdings.getExtendedHoldingsRecords().stream()
            .filter(
                holdingsRecord ->
                    duplicationCheckerFactory
                        .getFetchedIds(jobExecution)
                        .add(holdingsRecord.getEntity().getId()))
            .toList();

    var fetchedIds = duplicationCheckerFactory.getFetchedIds(jobExecution);
    var idsToAdd =
        distinctHoldings.stream()
            .map(extendedHoldingsRecord -> extendedHoldingsRecord.getEntity().getId())
            .toList();
    fetchedIds.addAll(idsToAdd);

    return distinctHoldings;
  }

  private ExtendedHoldingsRecordCollection getHoldingsRecords(ItemIdentifier itemIdentifier) {
    var type = IdentifierType.fromValue(identifierType);
    var identifier = itemIdentifier.getItemId();

    var instanceHrid = INSTANCE_HRID.equals(type) ? itemIdentifier.getItemId() : null;
    var itemBarcode = ITEM_BARCODE.equals(type) ? itemIdentifier.getItemId() : null;

    var centralTenantId = consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());

    if (isCurrentTenantCentral(centralTenantId)) {
      // Process central tenant
      var identifierTypeEnum = getSearchIdentifierType(type);
      var consortiumHoldingsCollection =
          searchClient.getConsortiumHoldingCollection(
              new BatchIdsDto()
                  .identifierType(getSearchIdentifierType(type))
                  .identifierValues(List.of(identifier)));

      if (consortiumHoldingsCollection != null
          && consortiumHoldingsCollection.getTotalRecords() > 0) {
        var extendedHoldingsRecordCollection =
            new ExtendedHoldingsRecordCollection()
                .withExtendedHoldingsRecords(new ArrayList<>())
                .withTotalRecords(0);

        var tenantIds =
            consortiumHoldingsCollection.getHoldings().stream()
                .map(ConsortiumHolding::getTenantId)
                .collect(Collectors.toSet());

        if (INSTANCEHRID != identifierTypeEnum && tenantIds.size() > 1) {
          throw new BulkEditException(DUPLICATES_ACROSS_TENANTS, ErrorType.ERROR);
        }

        var affiliatedPermittedTenants =
            tenantResolver.getAffiliatedPermittedTenantIds(
                HOLDINGS_RECORD, jobExecution, identifierType, tenantIds, itemIdentifier);

        affiliatedPermittedTenants.forEach(
            tenantId -> {
              try (@SuppressWarnings("unused")
                  var context =
                      new FolioExecutionContextSetter(
                          prepareContextForTenant(
                              tenantId, folioModuleMetadata, folioExecutionContext))) {
                var holdingsRecordCollection = getHoldingsRecordCollection(type, itemIdentifier);

                var toAdd =
                    holdingsRecordCollection.getHoldingsRecords().stream()
                        .map(holdingsRecord -> holdingsRecord.withInstanceHrid(instanceHrid))
                        .map(holdingsRecord -> holdingsRecord.withItemBarcode(itemBarcode))
                        .map(
                            holdingsRecord ->
                                holdingsRecord.withInstanceTitle(
                                    holdingsReferenceService.getInstanceTitleById(
                                        holdingsRecord.getInstanceId(), tenantId)))
                        .map(
                            holdingsRecord -> {
                              localReferenceDataService.enrichWithTenant(holdingsRecord, tenantId);
                              return holdingsRecord.withTenantId(tenantId);
                            })
                        .map(
                            holdingsRecord ->
                                new ExtendedHoldingsRecord()
                                    .withTenantId(tenantId)
                                    .withEntity(holdingsRecord))
                        .toList();

                extendedHoldingsRecordCollection.getExtendedHoldingsRecords().addAll(toAdd);
                extendedHoldingsRecordCollection.setTotalRecords(
                    extendedHoldingsRecordCollection.getTotalRecords()
                        + holdingsRecordCollection.getTotalRecords());
              } catch (Exception e) {
                log.error(e.getMessage());
                throw e;
              }
            });
        return extendedHoldingsRecordCollection;
      } else {
        throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
      }
    } else {
      // Process local tenant case
      checkReadPermissions(folioExecutionContext.getTenantId(), identifier);
      var holdingsRecordCollection = getHoldingsRecordCollection(type, itemIdentifier);

      var extendedHoldings =
          holdingsRecordCollection.getHoldingsRecords().stream()
              .map(holdingsRecord -> holdingsRecord.withInstanceHrid(instanceHrid))
              .map(holdingsRecord -> holdingsRecord.withItemBarcode(itemBarcode))
              .map(
                  holdingsRecord ->
                      holdingsRecord.withInstanceTitle(
                          holdingsReferenceService.getInstanceTitleById(
                              holdingsRecord.getInstanceId(), folioExecutionContext.getTenantId())))
              .map(
                  holdingsRecord ->
                      new ExtendedHoldingsRecord()
                          .withTenantId(folioExecutionContext.getTenantId())
                          .withEntity(holdingsRecord))
              .toList();

      var extendedHoldingsRecordCollection =
          new ExtendedHoldingsRecordCollection()
              .withExtendedHoldingsRecords(extendedHoldings)
              .withTotalRecords(holdingsRecordCollection.getTotalRecords());

      if (extendedHoldingsRecordCollection.getExtendedHoldingsRecords().isEmpty()) {
        throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
      }
      return extendedHoldingsRecordCollection;
    }
  }

  private void checkReadPermissions(String tenantId, String identifier) {
    if (!permissionsValidator.isBulkEditReadPermissionExists(tenantId, HOLDINGS_RECORD)) {
      var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
      throw new BulkEditException(
          format(
              NO_HOLDING_VIEW_PERMISSIONS,
              user.getUsername(),
              resolveIdentifier(identifierType),
              identifier,
              tenantId),
          ErrorType.ERROR);
    }
  }

  private boolean isCurrentTenantCentral(String centralTenantId) {
    return StringUtils.isNotEmpty(centralTenantId)
        && centralTenantId.equals(folioExecutionContext.getTenantId());
  }

  private HoldingsRecordCollection getHoldingsRecordCollection(
      IdentifierType type, ItemIdentifier itemIdentifier) {
    if (Set.of(ID, HRID).contains(type)) {
      var url =
          format(
              getMatchPattern(identifierType),
              resolveIdentifier(identifierType),
              itemIdentifier.getItemId());
      var holdingsRecordCollection = holdingsStorageClient.getByQuery(url);
      if (holdingsRecordCollection.getTotalRecords() > 1) {
        log.error(
            "Response from {} for tenant {}: {}",
            url,
            folioExecutionContext.getTenantId(),
            getResponseAsString(holdingsRecordCollection));
        throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE, ErrorType.ERROR);
      }
      return holdingsRecordCollection;
    } else if (INSTANCE_HRID == type) {
      return holdingsStorageClient.getByQuery(
          "instanceId==" + holdingsReferenceService.getInstanceIdByHrid(itemIdentifier.getItemId()),
          Integer.MAX_VALUE);
    } else if (ITEM_BARCODE == type) {
      return holdingsStorageClient.getByQuery(
          "id==" + holdingsReferenceService.getHoldingsIdByItemBarcode(itemIdentifier.getItemId()),
          1);
    } else {
      throw new BulkEditException(
          format("Identifier type \"%s\" is not supported", identifierType), ErrorType.ERROR);
    }
  }
}
