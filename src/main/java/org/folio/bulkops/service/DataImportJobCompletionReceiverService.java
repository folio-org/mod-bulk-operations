package org.folio.bulkops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;
import org.folio.bulkops.domain.dto.DataImportJobExecution;

import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class DataImportJobCompletionReceiverService {
  private final BulkOperationService bulkOperationService;
  private final BulkOperationRepository bulkOperationRepository;
  private final FolioModuleMetadata folioModuleMetadata;
  private final SystemUserScopedExecutionService executionService;

  @KafkaListener(
    containerFactory = "kafkaListenerContainerFactoryDI",
    topicPattern = "${application.kafka.topic-pattern-di}",
    groupId = "${application.kafka.group-id}")
  public void receiveJobExecutionUpdate(DataImportJobExecution dataImportJobExecution, @Headers Map<String, Object> messageHeaders) {
    var defaultFolioExecutionContext = DefaultFolioExecutionContext.fromMessageHeaders(folioModuleMetadata, messageHeaders);
    try (var context = new FolioExecutionContextSetter(defaultFolioExecutionContext)) {
      var importProfileId = dataImportJobExecution.getJobProfileInfo().getId();
      bulkOperationRepository.findByDataImportJobProfileId(importProfileId).ifPresent(bulkOperation ->
      {
        var tenantId = defaultFolioExecutionContext.getTenantId();
        executionService.executeSystemUserScoped(tenantId, () -> {
          bulkOperationService.processDataImportResult(bulkOperation);
          return null;
        });
      });
    } catch (Exception e) {
      log.error("Failed to process DI event: {}, reason: {}", dataImportJobExecution, e.getMessage());
    }
  }
}
