package org.folio.bulkops.batch.jobs;

import static java.util.Collections.emptyList;
import static org.folio.bulkops.domain.bean.JobParameterNames.AT_LEAST_ONE_MARC_EXISTS;
import static org.folio.bulkops.util.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.bulkops.util.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.bulkops.util.Constants.DUPLICATE_ENTRY;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE;
import static org.folio.bulkops.util.Constants.LINKED_DATA_SOURCE_IS_NOT_SUPPORTED;
import static org.folio.bulkops.util.Constants.MARC;
import static org.folio.bulkops.util.Constants.MULTIPLE_MATCHES_MESSAGE;
import static org.folio.bulkops.util.Constants.NO_INSTANCE_VIEW_PERMISSIONS;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.batch.jobs.processidentifiers.DuplicationCheckerFactory;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.exception.MarcValidationException;
import org.folio.bulkops.processor.EntityExtractor;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.bulkops.service.SrsService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditInstanceProcessor implements ItemProcessor<ItemIdentifier, List<ExtendedInstance>>, EntityExtractor {
  private final InstanceClient instanceClient;
  private final FolioExecutionContext folioExecutionContext;
  private final PermissionsValidator permissionsValidator;
  private final UserClient userClient;
  private final DuplicationCheckerFactory duplicationCheckerFactory;
  private final SrsService srsService;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;
  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;

  @Override
  public List<ExtendedInstance> process(ItemIdentifier itemIdentifier) throws BulkEditException {
    log.debug("Instance processor current thread: {}", Thread.currentThread().getName());
    try {
      if (!permissionsValidator.isBulkEditReadPermissionExists(folioExecutionContext.getTenantId(), EntityType.INSTANCE)) {
        var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
        throw new BulkEditException(NO_INSTANCE_VIEW_PERMISSIONS.formatted(user.getUsername(), resolveIdentifier(identifierType), itemIdentifier.getItemId(), folioExecutionContext.getTenantId()), ErrorType.ERROR);
      }
      if (!duplicationCheckerFactory.getIdentifiersToCheckDuplication(jobExecution).add(itemIdentifier)) {
        throw new BulkEditException(DUPLICATE_ENTRY, ErrorType.WARNING);
      }

      var instance = getInstance(itemIdentifier);

      if (LINKED_DATA_SOURCE.equals(instance.getSource())) {
        throw new BulkEditException(LINKED_DATA_SOURCE_IS_NOT_SUPPORTED, ErrorType.ERROR);
      }

      if (duplicationCheckerFactory.getFetchedIds(jobExecution).add(instance.getId())) {
        checkSrsInstance(instance);
        return List.of(new ExtendedInstance().withEntity(instance).withTenantId(folioExecutionContext.getTenantId()));
      }
      return emptyList();
    } catch (BulkEditException e) {
      throw e;
    } catch (Exception e) {
      throw new BulkEditException(e.getMessage(), ErrorType.ERROR);
    }
  }

  /**
   * Retrieves instance based on the instance identifier value. Currently, only ID and HRID are supported for instances.
   * ISBN and ISSN are not supported because they are not unique identifiers for instances.
   *
   * @param itemIdentifier the item identifier to use for retrieving instances
   * @return the instance
   * @throws BulkEditException if the identifier type is not supported
   */
  private Instance getInstance(ItemIdentifier itemIdentifier) {
    return switch (IdentifierType.fromValue(identifierType)) {
      case ID, HRID -> {
        var instances = instanceClient.getInstanceByQuery(String.format(getMatchPattern(identifierType), resolveIdentifier(identifierType), itemIdentifier.getItemId()), 1);
        if (instances.getTotalRecords() > 1) {
          log.error(MULTIPLE_MATCHES_MESSAGE);
          throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE, ErrorType.ERROR);
        } else if (instances.getTotalRecords() < 1 || instances.getInstances().isEmpty()) {
          log.error(NO_MATCH_FOUND_MESSAGE);
          throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
        }
        yield instances.getInstances().getFirst();
      }
      default -> throw new BulkEditException(String.format("Identifier type \"%s\" is not supported", identifierType), ErrorType.ERROR);
    };
  }

  private void checkSrsInstance(Instance instance) throws BulkEditException, IOException {
    if (MARC.equals(instance.getSource())) {
      if (!jobExecution.getExecutionContext().containsKey(AT_LEAST_ONE_MARC_EXISTS)) {
        jobExecution.getExecutionContext().put(AT_LEAST_ONE_MARC_EXISTS, true);
      }
      try {
        srsService.getMarcJsonString(instance.getId());
      } catch (MarcValidationException mve) {
        throw new BulkEditException(mve.getMessage());
      }
    }
  }
}
