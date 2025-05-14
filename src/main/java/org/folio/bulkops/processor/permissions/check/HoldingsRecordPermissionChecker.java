package org.folio.bulkops.processor.permissions.check;

import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.bulkops.util.Constants.DUPLICATES_ACROSS_TENANTS;
import static org.folio.bulkops.util.Constants.NO_HOLDING_VIEW_PERMISSIONS;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.client.SearchConsortium;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.dto.BatchIdsDto;
import org.folio.bulkops.exception.UploadFromQueryException;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.bean.ConsortiumHolding;
import org.folio.bulkops.exception.ReadPermissionException;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Log4j2
@RequiredArgsConstructor
public class HoldingsRecordPermissionChecker {

  private final TenantResolver tenantResolver;
  private final ConsortiaService consortiaService;
  private final FolioExecutionContext folioExecutionContext;
  private final SearchConsortium searchClient;
  private final UserClient userClient;
  private final PermissionsValidator permissionsValidator;

  public void checkPermissionsAndAffiliations(String itemIdentifier) throws UploadFromQueryException {
    var centralTenantId = consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());
    if (isCurrentTenantCentral(centralTenantId)) {
      // Process central tenant
      var consortiumHoldingsCollection = searchClient.getConsortiumHoldingCollection(new BatchIdsDto()
        .identifierType(BatchIdsDto.IdentifierTypeEnum.ID)
        .identifierValues(List.of(itemIdentifier)));
      if (!consortiumHoldingsCollection.getHoldings().isEmpty()) {
        var tenantIds = consortiumHoldingsCollection.getHoldings()
          .stream()
          .map(ConsortiumHolding::getTenantId).collect(Collectors.toSet());
        if (tenantIds.size() > 1) {
          throw new UploadFromQueryException(DUPLICATES_ACROSS_TENANTS, itemIdentifier);
        }
        tenantResolver.checkAffiliatedPermittedTenantIds(EntityType.HOLDINGS_RECORD, IdentifierType.ID.getValue(), tenantIds, itemIdentifier);
      } else {
        throw new UploadFromQueryException(NO_MATCH_FOUND_MESSAGE, itemIdentifier);
      }
    } else {
      // Process local tenant case
      checkReadPermissions(folioExecutionContext.getTenantId(), itemIdentifier);
    }
  }

  private void checkReadPermissions(String tenantId, String identifier) throws ReadPermissionException {
    if (!permissionsValidator.isBulkEditReadPermissionExists(tenantId, EntityType.HOLDINGS_RECORD)) {
      var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
      throw new ReadPermissionException(NO_HOLDING_VIEW_PERMISSIONS.formatted(user.getUsername(), IdentifierType.ID, identifier, tenantId), identifier);
    }
  }

  private boolean isCurrentTenantCentral(String centralTenantId) {
    return StringUtils.isNotEmpty(centralTenantId) && centralTenantId.equals(folioExecutionContext.getTenantId());
  }
}
