package org.folio.bulkops.service;

import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.domain.dto.DataImportJobExecution;
import org.folio.bulkops.repository.BulkOperationRepository;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class DataImportJobCompletionReceiverService {
  private final BulkOperationService bulkOperationService;
  private final BulkOperationRepository bulkOperationRepository;
  private final FolioModuleMetadata folioModuleMetadata;

  @KafkaListener(
      containerFactory = "kafkaListenerContainerFactoryDi",
      topicPattern = "${application.kafka.topic-pattern-di}",
      groupId = "${application.kafka.group-id}")
  public void receiveJobExecutionUpdate(
      DataImportJobExecution dataImportJobExecution, @Headers Map<String, Object> messageHeaders) {
    var defaultFolioExecutionContext =
        DefaultFolioExecutionContext.fromMessageHeaders(folioModuleMetadata, messageHeaders);
    try (var context = new FolioExecutionContextSetter(defaultFolioExecutionContext)) {
      // reference context to avoid 'variable is never used' warning
      Objects.requireNonNull(context);

      var jobProfileInfo = dataImportJobExecution.getJobProfileInfo();
      if (jobProfileInfo == null) {
        log.warn("Received DI event without jobProfileInfo: {}", dataImportJobExecution);
        return;
      }

      var importProfileId = jobProfileInfo.getId();

      bulkOperationRepository
          .findByDataImportJobProfileId(importProfileId)
          .ifPresent(bulkOperationService::processDataImportResult);
    } catch (Exception e) {
      log.error(
          "Failed to process DI event: {}, reason: {}", dataImportJobExecution, e.getMessage());
    }
  }
}
