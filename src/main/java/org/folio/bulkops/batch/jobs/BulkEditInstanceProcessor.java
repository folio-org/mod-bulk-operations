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
import static org.folio.bulkops.util.Constants.MULTIPLE_SRS;
import static org.folio.bulkops.util.Constants.NO_INSTANCE_VIEW_PERMISSIONS;
import static org.folio.bulkops.util.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.bulkops.util.Constants.SRS_MISSING;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.bulkops.batch.jobs.processidentifiers.DuplicationCheckerFactory;
import org.folio.bulkops.client.InstanceClient;
import org.folio.bulkops.client.SrsClient;
import org.folio.bulkops.client.UserClient;
import org.folio.bulkops.domain.bean.ExtendedInstance;
import org.folio.bulkops.domain.dto.EntityType;
import org.folio.bulkops.domain.dto.ErrorType;
import org.folio.bulkops.domain.dto.IdentifierType;
import org.folio.bulkops.domain.bean.Instance;
import org.folio.bulkops.domain.bean.ItemIdentifier;
import org.folio.bulkops.exception.BulkEditException;
import org.folio.bulkops.processor.EntityExtractor;
import org.folio.bulkops.processor.permissions.check.PermissionsValidator;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.StreamSupport;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditInstanceProcessor implements ItemProcessor<ItemIdentifier, List<ExtendedInstance>>, EntityExtractor {
  private final InstanceClient instanceClient;
  private final FolioExecutionContext folioExecutionContext;
  private final PermissionsValidator permissionsValidator;
  private final UserClient userClient;
  private final SrsClient srsClient;
  private final DuplicationCheckerFactory duplicationCheckerFactory;

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
        checkMissingSrsAndDuplication(instance);
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

  private void checkMissingSrsAndDuplication(Instance instance) {
    if (MARC.equals(instance.getSource())) {
      if (!jobExecution.getExecutionContext().containsKey(AT_LEAST_ONE_MARC_EXISTS)) {
        jobExecution.getExecutionContext().put(AT_LEAST_ONE_MARC_EXISTS, true);
      }
      var srsRecords = srsClient.getMarc(instance.getId(), "INSTANCE", true).get("sourceRecords");
      if (srsRecords.isEmpty()) {
        log.error(SRS_MISSING);
        throw new BulkEditException(SRS_MISSING);
      }
      if (srsRecords.size() > 1) {
        var errorMsg = MULTIPLE_SRS.formatted(getAllSrsIds(srsRecords));
        log.error(errorMsg);
        throw new BulkEditException(errorMsg);
      }
    }
  }

  private String getAllSrsIds(JsonNode srsRecords) {
    return String.join(", ", StreamSupport.stream(srsRecords.spliterator(), false)
      .map(n -> StringUtils.strip(n.get("recordId").toString(), "\"")).toList());
  }
}
