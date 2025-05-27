package org.folio.bulkops.batch.jobs;

import static java.util.Optional.ofNullable;
import static org.folio.bulkops.domain.bean.JobParameterNames.BULK_OPERATION_ID;
import static org.folio.bulkops.util.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.bulkops.util.Constants.NO_HOLDING_AFFILIATION;
import static org.folio.bulkops.util.Constants.NO_HOLDING_VIEW_PERMISSIONS;
import static org.folio.bulkops.util.Constants.NO_ITEM_AFFILIATION;
import static org.folio.bulkops.util.Constants.NO_ITEM_VIEW_PERMISSIONS;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.service.ConsortiaService;
import org.folio.bulkops.service.ErrorService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantResolver {

  private static final String UNSUPPORTED_ERROR_MESSAGE_FOR_AFFILIATIONS = "Unsupported entity type to get affiliation error message";
  private static final String UNSUPPORTED_ERROR_MESSAGE_FOR_PERMISSIONS = "Unsupported entity type to get permissions error message";

  private final FolioExecutionContext folioExecutionContext;
  private final ConsortiaService consortiaService;
  private final PermissionsValidator permissionsValidator;
  private final ErrorService errorService;
  private final UserClient userClient;

  public Set<String> getAffiliatedPermittedTenantIds(EntityType entityType, JobExecution jobExecution, String identifierType, Set<String> tenantIds, ItemIdentifier itemIdentifier) {
    var affiliatedTenants = consortiaService.getAffiliatedTenants(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
    var bulkOperationId = ofNullable(jobExecution.getJobParameters().getString(BULK_OPERATION_ID)).map(UUID::fromString).orElse(null);
    var affiliatedAndPermittedTenants = new HashSet<String>();
    for (var tenantId : tenantIds) {
      if (!affiliatedTenants.contains(tenantId)) {
        var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
        var errorMessage = getAffiliationErrorPlaceholder(entityType).formatted(user.getUsername(),
          resolveIdentifier(identifierType), itemIdentifier.getItemId(), tenantId);
        errorService.saveError(bulkOperationId, itemIdentifier.getItemId(), errorMessage, ErrorType.ERROR);
      } else if (!isBulkEditReadPermissionExists(tenantId, entityType)) {
        var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
        var errorMessage = getViewPermissionErrorPlaceholder(entityType).formatted(user.getUsername(),
          resolveIdentifier(identifierType), itemIdentifier.getItemId(), tenantId);
        errorService.saveError(bulkOperationId, itemIdentifier.getItemId(), errorMessage, ErrorType.ERROR);
      } else {
        affiliatedAndPermittedTenants.add(tenantId);
      }
    }
    return affiliatedAndPermittedTenants;
  }

  private boolean isBulkEditReadPermissionExists(String tenantId, EntityType entityType) {
    try {
      return permissionsValidator.isBulkEditReadPermissionExists(tenantId, entityType);
    } catch (FeignException e) {
      throw new BulkEditException(e.getMessage(), ErrorType.ERROR);
    }
  }

  protected String getAffiliationErrorPlaceholder(EntityType entityType) {
    return switch (entityType) {
      case ITEM -> NO_ITEM_AFFILIATION;
      case HOLDINGS_RECORD -> NO_HOLDING_AFFILIATION;
      default -> throw new UnsupportedOperationException(UNSUPPORTED_ERROR_MESSAGE_FOR_AFFILIATIONS);
    };
  }

  protected String getViewPermissionErrorPlaceholder(EntityType entityType) {
    return switch (entityType) {
      case ITEM -> NO_ITEM_VIEW_PERMISSIONS;
      case HOLDINGS_RECORD -> NO_HOLDING_VIEW_PERMISSIONS;
      default -> throw new UnsupportedOperationException(UNSUPPORTED_ERROR_MESSAGE_FOR_PERMISSIONS);
    };
  }
}
