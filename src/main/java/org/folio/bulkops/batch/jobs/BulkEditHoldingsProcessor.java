package org.folio.bulkops.batch.jobs;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.folio.bulkops.domain.dto.BatchIdsDto.IdentifierTypeEnum.INSTANCEHRID;
import static org.folio.bulkops.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.bulkops.domain.dto.IdentifierType.HRID;
import static org.folio.bulkops.domain.dto.IdentifierType.ID;
import static org.folio.bulkops.domain.dto.IdentifierType.INSTANCE_HRID;
import static org.folio.bulkops.domain.dto.IdentifierType.ITEM_BARCODE;
import static org.folio.bulkops.util.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.bulkops.util.BulkEditProcessorHelper.getResponseAsString;
import static org.folio.bulkops.util.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.bulkops.util.Constants.ARRAY_DELIMITER;
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
import org.folio.bulkops.client.HoldingsClient;
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
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditHoldingsProcessor implements ItemProcessor<ItemIdentifier, List<ExtendedHoldingsRecord>>, EntityExtractor {
  private final HoldingsClient holdingClient;
  private final HoldingsReferenceService holdingsReferenceService;
  private final SearchClient searchClient;
  private final ConsortiaService consortiaService;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private final UserClient userClient;
  private final PermissionsValidator permissionsValidator;
  private final TenantResolver tenantResolver;
  private final DuplicationCheckerFactory duplicationCheckerFactory;

  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;
  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;

  @Override
  public List<ExtendedHoldingsRecord> process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (!duplicationCheckerFactory.getIdentifiersToCheckDuplication(jobExecution).add(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry", ErrorType.WARNING);
    }

    var holdings = getHoldingsRecords(itemIdentifier);
    var distinctHoldings = holdings.getExtendedHoldingsRecords().stream()
      .filter(holdingsRecord -> duplicationCheckerFactory.getFetchedIds(jobExecution).add(holdingsRecord.getEntity().getId()))
      .toList();
    duplicationCheckerFactory.getFetchedIds(jobExecution).addAll(distinctHoldings.stream().map(extendedHoldingsRecord -> extendedHoldingsRecord.getEntity().getId()).toList());

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
      var consortiumHoldingsCollection = searchClient.getConsortiumHoldingCollection(new BatchIdsDto()
          .identifierType(getSearchIdentifierType(type))
        .identifierValues(List.of(identifier)));
      if (consortiumHoldingsCollection.getTotalRecords() > 0) {
        var extendedHoldingsRecordCollection = new ExtendedHoldingsRecordCollection()
          .withExtendedHoldingsRecords(new ArrayList<>())
          .withTotalRecords(0);
        var tenantIds = consortiumHoldingsCollection.getHoldings()
          .stream()
          .map(ConsortiumHolding::getTenantId).collect(Collectors.toSet());
        if (INSTANCEHRID != identifierTypeEnum && tenantIds.size() > 1) {
          throw new BulkEditException(DUPLICATES_ACROSS_TENANTS, ErrorType.ERROR);
        }
        var affiliatedPermittedTenants = tenantResolver.getAffiliatedPermittedTenantIds(HOLDINGS_RECORD,
          jobExecution, identifierType, tenantIds, itemIdentifier);
        affiliatedPermittedTenants.forEach(tenantId -> {
          try (var context = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
            var holdingsRecordCollection = getHoldingsRecordCollection(type, itemIdentifier);
            extendedHoldingsRecordCollection.getExtendedHoldingsRecords().addAll(
              holdingsRecordCollection.getHoldingsRecords().stream()
                .map(holdingsRecord -> holdingsRecord.withInstanceHrid(instanceHrid))
                .map(holdingsRecord -> holdingsRecord.withItemBarcode(itemBarcode))
                .map(holdingsRecord -> holdingsRecord.withInstanceTitle(holdingsReferenceService.getInstanceTitleById(holdingsRecord.getInstanceId(), tenantId)))
                .map(holdingsRecord -> {
                  if (nonNull(holdingsRecord.getElectronicAccess())) {
                    holdingsRecord.getElectronicAccess().forEach(el -> el.setTenantId(tenantId));
                  }
                  if (nonNull(holdingsRecord.getNotes())) {
                    holdingsRecord.getNotes().forEach(note -> note.setTenantId(tenantId));
                  }
                  if (nonNull(holdingsRecord.getStatisticalCodeIds())) {
                    holdingsRecord.setStatisticalCodeIds(holdingsRecord.getStatisticalCodeIds().stream().map(stat -> stat + ARRAY_DELIMITER + tenantId).toList());
                  }
                  if (nonNull(holdingsRecord.getIllPolicyId())) {
                    holdingsRecord.setIllPolicyId(holdingsRecord.getIllPolicyId() + ARRAY_DELIMITER + tenantId);
                  }
                  if (nonNull(holdingsRecord.getEffectiveLocationId())) {
                    holdingsRecord.setEffectiveLocationId(holdingsRecord.getEffectiveLocationId() + ARRAY_DELIMITER + tenantId);
                  }
                  if (nonNull(holdingsRecord.getPermanentLocationId())) {
                    holdingsRecord.setPermanentLocationId(holdingsRecord.getPermanentLocationId() + ARRAY_DELIMITER + tenantId);
                  }
                  if (nonNull(holdingsRecord.getSourceId())) {
                    holdingsRecord.setSourceId(holdingsRecord.getSourceId() + ARRAY_DELIMITER + tenantId);
                  }
                  if (nonNull(holdingsRecord.getHoldingsTypeId())) {
                    holdingsRecord.setHoldingsTypeId(holdingsRecord.getHoldingsTypeId() + ARRAY_DELIMITER + tenantId);
                  }
                  if (nonNull(holdingsRecord.getTemporaryLocationId())) {
                    holdingsRecord.setTemporaryLocationId(holdingsRecord.getTemporaryLocationId() + ARRAY_DELIMITER + tenantId);
                  }
                  return holdingsRecord.withTenantId(tenantId);
                })
                .map(holdingsRecord -> new ExtendedHoldingsRecord().withTenantId(tenantId).withEntity(holdingsRecord)).toList()
            );
            extendedHoldingsRecordCollection.setTotalRecords(extendedHoldingsRecordCollection.getTotalRecords() + holdingsRecordCollection.getTotalRecords());
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
      var extendedHoldingsRecordCollection =  new ExtendedHoldingsRecordCollection().withExtendedHoldingsRecords(holdingsRecordCollection.getHoldingsRecords().stream()
          .map(holdingsRecord -> holdingsRecord.withInstanceHrid(instanceHrid))
          .map(holdingsRecord -> holdingsRecord.withItemBarcode(itemBarcode))
          .map(holdingsRecord -> holdingsRecord.withInstanceTitle(holdingsReferenceService.getInstanceTitleById(holdingsRecord.getInstanceId(), folioExecutionContext.getTenantId())))
          .map(holdingsRecord -> new ExtendedHoldingsRecord().withTenantId(folioExecutionContext.getTenantId()).withEntity(holdingsRecord)).toList())
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
      throw new BulkEditException(format(NO_HOLDING_VIEW_PERMISSIONS, user.getUsername(), resolveIdentifier(identifierType), identifier, tenantId), ErrorType.ERROR);
    }
  }

  private boolean isCurrentTenantCentral(String centralTenantId) {
    return StringUtils.isNotEmpty(centralTenantId) && centralTenantId.equals(folioExecutionContext.getTenantId());
  }

  private HoldingsRecordCollection getHoldingsRecordCollection(IdentifierType type, ItemIdentifier itemIdentifier) {
    if (Set.of(ID, HRID).contains(type)) {
      var url = format(getMatchPattern(identifierType), resolveIdentifier(identifierType), itemIdentifier.getItemId());
      var holdingsRecordCollection = holdingClient.getByQuery(url);
      if (holdingsRecordCollection.getTotalRecords() > 1) {
        log.error("Response from {} for tenant {}: {}", url, folioExecutionContext.getTenantId(), getResponseAsString(holdingsRecordCollection));
        throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE, ErrorType.ERROR);
      }
      return holdingsRecordCollection;
    } else if (INSTANCE_HRID == type) {
      return holdingClient.getByQuery("instanceId==" + holdingsReferenceService.getInstanceIdByHrid(itemIdentifier.getItemId()), Integer.MAX_VALUE);
    } else if (ITEM_BARCODE == type) {
      return holdingClient.getByQuery("id==" + holdingsReferenceService.getHoldingsIdByItemBarcode(itemIdentifier.getItemId()), 1);
    } else {
      throw new BulkEditException(format("Identifier type \"%s\" is not supported", identifierType), ErrorType.ERROR);
    }
  }
}
